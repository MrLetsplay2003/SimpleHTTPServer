package me.mrletsplay.simplehttpserver.http.server.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
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
import me.mrletsplay.simplehttpserver.util.BufferUtil;

public class HttpsConnection extends HttpConnection {

	private SSLEngine engine;

	private boolean handshakeDone;

	private ByteBuffer
		myAppData, // Read
		myNetData, // Read
		peerAppData, // Write
		peerNetData; // Write

	public HttpsConnection(HttpServer server, SocketChannel socket, SSLContext sslContext) {
		super(server, socket);

		handshakeDone = false;

		engine = sslContext.createSSLEngine();
		engine.setUseClientMode(false);

		SSLSession session = engine.getSession();
		myAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
		myAppData.limit(0);
		myNetData = ByteBuffer.allocate(session.getPacketBufferSize());
		myNetData.limit(0);
		peerAppData = ByteBuffer.allocate(session.getApplicationBufferSize());
		peerNetData = ByteBuffer.allocate(session.getPacketBufferSize());

		try {
			engine.beginHandshake();
		} catch (SSLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean isSecure() {
		return true;
	}

	private void processAppData() throws IOException {
		if(!getSelectionKey().isValid()) return;

		int ops = getSelectionKey().interestOps();
		boolean read = (ops & SelectionKey.OP_READ) == SelectionKey.OP_READ;
		boolean write = (ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE;

		System.out.printf("Process read: %s, write: %s\n", read, write);

		peerAppData.flip();
		System.out.println("Read: " + read + ", " + peerAppData.remaining());
		if(read && peerAppData.hasRemaining()) {
			readData(peerAppData);
		}
		peerAppData.compact();

		myAppData.compact();
		if(write && myAppData.hasRemaining()) {
			writeData(myAppData);
		}
		myAppData.flip();
		System.out.println("AFTER WRITE: " + myAppData.remaining());
	}

	@Override
	public boolean canReadData() {
		return super.canReadData() || peerNetData.hasRemaining();
	}

	@Override
	public void readData() throws IOException {
		System.out.println("--- R: " + engine.getHandshakeStatus());

		if(!handshakeDone) {
			performHandshake(true, false);
			setHandshakeInterestOps();
			return;
		}

		if(peerNetData.remaining() == 0) throw new IOException("Buffer is full");
		if(getSocket().read(peerNetData) == -1) {
			close();
			return;
		}

		unwrapPeerData();
		processAppData();
	}

	protected void unwrapPeerData() throws IOException {
		peerNetData.flip();
		while(peerNetData.hasRemaining()) {
			System.out.println(peerNetData.remaining());
			SSLEngineResult res = engine.unwrap(peerNetData, peerAppData);
			if(res.getStatus() != Status.OK) {
				System.out.println(res.getStatus());
				handleHandshakeStatus(res.getStatus());
				break;
			}
		}
		peerNetData.compact();
		System.out.println("read: " + new String(peerAppData.array(), 0, peerAppData.position()));
	}

	@Override
	public boolean canWriteData() {
		return super.canWriteData() || myNetData.hasRemaining();
	}

	@Override
	public void writeData() throws IOException {
		System.out.println("--- W: " + engine.getHandshakeStatus());

		if(!handshakeDone) {
			performHandshake(false, true);
			setHandshakeInterestOps();
			return;
		}

		processAppData();
		wrapMyData();

		System.out.println("W: " + myNetData.remaining());
		if(getSocket().write(myNetData) == -1) {
			close();
			return;
		}
		myNetData.compact();

		if(!myNetData.hasRemaining()) {
			finishWrite();
		}
	}

	protected void wrapMyData() throws IOException {
		myNetData.compact();
		while(myAppData.hasRemaining()) {
			System.out.println("FILLING: " + myAppData.remaining());
			System.out.println(new String(myAppData.array(), myAppData.position(), myAppData.remaining(), StandardCharsets.UTF_8));
			SSLEngineResult res = engine.wrap(myAppData, myNetData);
			if(res.getStatus() != Status.OK) {
				handleHandshakeStatus(res.getStatus());
				break;
			}
		}
		myNetData.flip();
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
			if(getSocket().write(myNetData) == -1) {
				close();
				return;
			}
			myNetData.compact();
			myNetData.flip();

			if(!myNetData.hasRemaining() && engine.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING) {
				finishHandshake();
			}

			return;
		}

		if (canRead && getSocket().read(peerNetData) == -1) {
			close();
			return;
		}

		while(engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
			System.out.println("HS: " + engine.getHandshakeStatus());
			switch (engine.getHandshakeStatus()) {
				case NEED_UNWRAP:
				{
					peerNetData.flip();
					SSLEngineResult res = engine.unwrap(peerNetData, peerAppData);
					peerNetData.compact();

					if (res.getStatus() != Status.OK) {
						System.out.println("E: " + res.getStatus());
						handleHandshakeStatus(res.getStatus());
						return;
					}

					break;
				}
				case NEED_WRAP:
				{
					myNetData.compact();
					SSLEngineResult res = engine.wrap(myAppData, myNetData);
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
					peerAppData = BufferUtil.reallocate(peerAppData, engine.getSession().getApplicationBufferSize());
				}

				break;
			}
			case BUFFER_UNDERFLOW:
				if (engine.getSession().getPacketBufferSize() > peerNetData.capacity()) {
					peerNetData = BufferUtil.reallocate(peerNetData, engine.getSession().getPacketBufferSize());
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
