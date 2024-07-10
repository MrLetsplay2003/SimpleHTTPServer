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

	public HttpsConnectionImpl(HttpServer server, SocketChannel socket, SSLContext sslContext) {
		super(server, socket);

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

		int ops = getSelectionKey().interestOps();
		boolean read = (ops & SelectionKey.OP_READ) == SelectionKey.OP_READ;
		boolean write = (ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE;

		System.out.printf("Process read: %s, write: %s\n", read, write);

		unwrapPeerData();
		peerAppData.flip();
		System.out.println("Read: " + read + ", " + peerAppData.remaining());
		if(read && peerAppData.hasRemaining()) {
			dataProcessor.readData(peerAppData.read());
		}
		peerAppData.flip();

		myAppData.flip();
		if(write && myAppData.hasRemaining()) {
			dataProcessor.writeData(myAppData.write());
		}
		myAppData.flip();
		wrapMyData();
		System.out.println("AFTER WRITE: " + myAppData.remaining());
	}

	@Override
	public void readData() throws IOException {
		System.out.println("--- R: " + engine.getHandshakeStatus());

		if(!handshakeDone) {
			performHandshake(true, false);
			setHandshakeInterestOps();
			System.out.println(handshakeDone);
			if(!handshakeDone) return;
		}

		if(peerNetData.remaining() == 0) throw new IOException("Buffer is full");
		if(getSocket().read(peerNetData.write()) == -1) {
			close();
			return;
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

		System.out.println("read: " + peerAppData);
	}

	@Override
	public void writeData() throws IOException {
		System.out.println("--- W: " + engine.getHandshakeStatus());

		if(!handshakeDone) {
			performHandshake(false, true);
			setHandshakeInterestOps();
			if(!handshakeDone) return;
		}

		processAppData();

		System.out.println("W: " + myNetData.remaining());
		if(getSocket().write(myNetData.read()) == -1) {
			close();
			return;
		}

		if(!myNetData.hasRemaining()) {
			stopWriting();
		}
	}

	protected void wrapMyData() throws IOException {
		myNetData.flip();
		if(!myNetData.hasRemaining()) {
			myNetData.flip();
			return;
		}

		System.out.println("FILLING: " + myAppData.remaining() + " -> " + myNetData.remaining());
		System.out.println(myAppData);
		SSLEngineResult res = engine.wrap(myAppData.read(), myNetData.write());
		myNetData.flip();
		System.out.println("Remaining after fill: " + myNetData.remaining());

		if(res.getStatus() != Status.OK) {
			System.out.println("NOT OK: " + res.getStatus());
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
		handshakeDone = true;
		getSelectionKey().interestOps(SelectionKey.OP_READ);
	}

	protected void performHandshake(boolean canRead, boolean canWrite) throws IOException {
		if (canWrite && myNetData.hasRemaining()) {
			if(getSocket().write(myNetData.read()) == -1) {
				close();
				return;
			}
//			myNetData.compact();
//			myNetData.flip();

			if(!myNetData.hasRemaining() && engine.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING) {
				finishHandshake();
			}

			return;
		}

		if (canRead && getSocket().read(peerNetData.write()) == -1) {
			close();
			return;
		}

		while(engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
			System.out.println("HS: " + engine.getHandshakeStatus());
			switch (engine.getHandshakeStatus()) {
				case NEED_UNWRAP:
				{
					peerNetData.flip();
					SSLEngineResult res = engine.unwrap(peerNetData.read(), peerAppData.write());
					peerNetData.flip();

					if (res.getStatus() != Status.OK) {
						System.out.println("E: " + res.getStatus());
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

					if (res.getStatus() != Status.OK) {
						System.out.println("E: " + res.getStatus());
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
								getServer().getSelector().wakeup();
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
	}

	private void handleHandshakeStatus(SSLEngineResult.Status status) {
		switch(status) {
			case BUFFER_OVERFLOW:
			{
				if (engine.getSession().getApplicationBufferSize() > peerAppData.capacity()) {
					peerAppData.reallocate(engine.getSession().getApplicationBufferSize());
				}

				break;
			}
			case BUFFER_UNDERFLOW:
				if (engine.getSession().getPacketBufferSize() > peerNetData.capacity()) {
					peerNetData.reallocate(engine.getSession().getPacketBufferSize());
				}

				break;
			case CLOSED:
				close();
				break;
			case OK:
			default:
				break;
		}
	}

}
