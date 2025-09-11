package me.mrletsplay.simplehttpserver.http.server.connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import me.mrletsplay.simplehttpserver.http.server.HttpServer;
import me.mrletsplay.simplehttpserver.http.websocket.WebSocketConnection;
import me.mrletsplay.simplehttpserver.server.connection.AbstractConnection;
import me.mrletsplay.simplehttpserver.util.RWBuffer;

public class HttpsConnectionImpl extends AbstractConnection implements HttpConnection {

	private SSLEngine engine;

	private boolean handshakeDone;

	private RWBuffer
		myAppData, // Read, outbound unencrypted data
		myNetData, // Read, outbound encrypted data
		peerAppData, // Write, inbound unencrypted data
		peerNetData; // Write, inbound encrypted data

	private HttpDataProcessor dataProcessor;

	public HttpsConnectionImpl(HttpServer server, SelectionKey selectionKey, SocketChannel socket, SSLContext sslContext) {
		super(server, selectionKey, socket);

		handshakeDone = false;

		engine = sslContext.createSSLEngine();
		engine.setUseClientMode(false);

		SSLSession session = engine.getSession();
		myAppData = RWBuffer.readableBuffer(session.getApplicationBufferSize());
		myNetData = RWBuffer.readableBuffer(session.getPacketBufferSize());
		peerAppData = RWBuffer.writableBuffer(session.getApplicationBufferSize());
		peerNetData = RWBuffer.writableBuffer(session.getPacketBufferSize());

		try {
			engine.beginHandshake();
		} catch (SSLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		dataProcessor = new HttpDataProcessor(this);
	}

	@Override
	public boolean isSecure() {
		return true;
	}

	@Override
	public HttpServer getServer() {
		return (HttpServer) super.getServer();
	}

	@Override
	public void setWebsocketConnection(WebSocketConnection connection) {
		dataProcessor.setWebsocketConnection(connection);
	}

	@Override
	public WebSocketConnection getWebsocketConnection() {
		return dataProcessor.getWebsocketConnection();
	}

	private void processAppData() throws IOException {
		if(!getSelectionKey().isValid()) return;

		getLogger().trace(String.format("Processing app data"));

		unwrapPeerData();
		peerAppData.flip();
		if(peerAppData.hasRemaining()) {
			getLogger().trace("Bytes to read: " + peerAppData.remaining());
			dataProcessor.readData(peerAppData.read());
		}
		peerAppData.flip();

		myAppData.flip();
		if(myAppData.hasRemaining()) {
			dataProcessor.writeData(myAppData.write());
		}
		myAppData.flip();
		wrapMyData();
	}

	@Override
	public void readData() throws IOException {
		getLogger().trace("Reading data from socket. Status: " + engine.getHandshakeStatus());

		if(peerNetData.remaining() == 0) throw new IOException("Buffer is full");
		if(getSocket().read(peerNetData.write()) == -1) {
			shutdownRead();
			return;
		}

		if(!handshakeDone) {
			performHandshake(true, false);
			setHandshakeInterestOps();
			if(!handshakeDone) return;
		}

		processAppData();
	}

	protected void unwrapPeerData() throws IOException {
		peerNetData.flip();
		SSLEngineResult res = engine.unwrap(peerNetData.read(), peerAppData.write());
		peerNetData.flip();

		if(res.getStatus() != Status.OK) {
			handleHandshakeStatus(res.getStatus());
		}

		getLogger().trace(String.format("Unwrapped app data:\n\n%s\n\n", peerAppData));
	}

	@Override
	public void writeData() throws IOException {
		getLogger().trace(String.format("Writing data to socket. Status: " + engine.getHandshakeStatus()));

		if(!handshakeDone) {
			performHandshake(false, true);
			setHandshakeInterestOps();
		}

		if(handshakeDone) {
			processAppData();
		}

		if(!myNetData.hasRemaining()) {
			stopWriting();
			return;
		}

		getLogger().trace(String.format("Can write up to %s bytes", myNetData.remaining()));
		if(getSocket().write(myNetData.read()) == -1) {
			shutdownWrite();
			return;
		}
	}

	protected void wrapMyData() throws IOException {
		myNetData.flip();
		if(!myNetData.hasRemaining()) {
			myNetData.flip();
			return;
		}

		getLogger().trace(String.format("Wrapping app data (%s bytes, buffer size remaining: %s)", myAppData.remaining(), myNetData.remaining()));
		getLogger().trace(String.format("Data to wrap:\n\n%s\n\n", myAppData));
		SSLEngineResult res = engine.wrap(myAppData.read(), myNetData.write());
		myNetData.flip();
		getLogger().trace(String.format("Buffer size remaining after wrapping: %s", myNetData.remaining()));

		if(res.getStatus() != Status.OK) {
			getLogger().debug(String.format("Got non-OK status after wrapping: %s", res.getStatus()));
			handleHandshakeStatus(res.getStatus());
			return;
		}
	}

	protected void setHandshakeInterestOps() {
		SelectionKey key = getSelectionKey();

		switch(engine.getHandshakeStatus()) {
			case NEED_TASK:
				key.interestOps(0);
				break;
			case NEED_UNWRAP:
				key.interestOps(SelectionKey.OP_READ);
				break;
			case NEED_WRAP:
				key.interestOps(SelectionKey.OP_WRITE);
				break;
			default:
				break;
		}

		if(myNetData.hasRemaining()) key.interestOpsOr(SelectionKey.OP_WRITE);
	}

	protected void finishHandshake() {
		getLogger().debug("Handshake finished");
		handshakeDone = true;
		getSelectionKey().interestOps(SelectionKey.OP_READ);
	}

	protected void performHandshake(boolean canRead, boolean canWrite) throws IOException {
		while(engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
			getLogger().debug(String.format("Performing handshake operation. Status: %s", engine.getHandshakeStatus()));

			switch (engine.getHandshakeStatus()) {
				case NEED_UNWRAP:
				{
					peerNetData.flip();
					SSLEngineResult res = engine.unwrap(peerNetData.read(), peerAppData.write());
					peerNetData.flip();

					if (res.getStatus() != Status.OK) {
						getLogger().debug(String.format("Got non-OK status after handshake unwrap: %s", res.getStatus()));
						handleHandshakeStatus(res.getStatus());
						return;
					}

					break;
				}
				case NEED_WRAP:
				{
					myNetData.flip();
					SSLEngineResult res = engine.wrap(myAppData.read(), myNetData.write());
					myNetData.flip();
					startWriting();

					if (res.getStatus() != Status.OK) {
						getLogger().debug(String.format("Got non-OK status after handshake wrap: %s", res.getStatus()));
						handleHandshakeStatus(res.getStatus());
						return;
					}

					break;
				}
				case NEED_TASK:
				{
					Runnable task;
					AtomicInteger remaining = new AtomicInteger();
					List<Runnable> tasks = new ArrayList<>();
					while ((task = engine.getDelegatedTask()) != null) {
						tasks.add(task);
						remaining.incrementAndGet();
					}

					for(Runnable t : tasks) {
						getServer().getExecutor().submit(() -> {
							t.run();

							if(remaining.decrementAndGet() == 0) {
								setHandshakeInterestOps();
								getSelectionKey().selector().wakeup();
							}
						});
					}

					return;
				}
				default:
					// Doesn't apply here
					return;
			}
		}

		if(engine.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING) {
			finishHandshake();
		}
	}

	private void handleHandshakeStatus(SSLEngineResult.Status status) {
		switch(status) {
			case BUFFER_OVERFLOW:
			{
				int desiredBufferSize = engine.getSession().getApplicationBufferSize();
				if (desiredBufferSize > peerAppData.capacity()) {
					getLogger().debug(String.format("Reallocating peer app data buffer with new size: %s", desiredBufferSize));
					peerAppData.reallocate(desiredBufferSize);
				}

				break;
			}
			case BUFFER_UNDERFLOW:
			{
				int desiredBufferSize = engine.getSession().getPacketBufferSize();
				if (desiredBufferSize > peerNetData.capacity()) {
					getLogger().debug(String.format("Reallocating peer net data buffer with new size: %s", desiredBufferSize));
					peerNetData.reallocate(desiredBufferSize);
				}

				break;
			}
			case CLOSED:
				close();
				break;
			case OK:
			default:
				break;
		}
	}

}
