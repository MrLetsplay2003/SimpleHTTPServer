package me.mrletsplay.simplehttpserver.http.server.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
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
import me.mrletsplay.simplehttpserver.util.BufferUtil;

public class HttpsConnection extends HttpConnection {

	private SSLEngine engine;

	private ByteBuffer myAppData, myNetData, peerAppData, peerNetData;

	public HttpsConnection(HttpServer server, SocketChannel socket, SSLContext sslContext) {
		super(server, socket);

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

	@Override
	public boolean canReadData() {
		return super.canReadData() || peerNetData.hasRemaining();
	}

	@Override
	public void readData() throws IOException {
		System.out.println("R: " + engine.getHandshakeStatus());

		if(engine.getHandshakeStatus() != HandshakeStatus.FINISHED
			&& engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
			performHandshake(true, false);
			setHandshakeInterestOps();
			return;
		}

		if(peerNetData.remaining() == 0) throw new IOException("Buffer is full");
		if(getSocket().read(peerNetData) == -1) {
			close();
			return;
		}

		peerNetData.flip();
		convertPeerData();
		peerNetData.compact();
	}

	protected void convertPeerData() throws IOException {
		while((getSelectionKey().interestOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ && peerNetData.hasRemaining()) {
			System.out.println(peerNetData.remaining());
			SSLEngineResult res = engine.unwrap(peerNetData, peerAppData);
			if(res.getStatus() != Status.OK) {
				System.out.println(res.getStatus());
				handleHandshakeStatus(res.getStatus());
				return;
			}


			peerAppData.flip();
			System.out.println(new String(peerAppData.array()));
			// TODO: turn read/write routine into generic loop for both so it behaves like selectionkey would (check flags and only run selected methods)
			readData(peerAppData);
			peerAppData.compact();
		}
	}

	@Override
	public boolean canWriteData() {
		System.out.println("CanWrite");
		return super.canWriteData() || myNetData.hasRemaining();
	}

	@Override
	public void writeData() throws IOException {
		System.out.println("W: " + engine.getHandshakeStatus());

		if(engine.getHandshakeStatus() != HandshakeStatus.FINISHED
			&& engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
			performHandshake(false, true);
			setHandshakeInterestOps();
			return;
		}

		if(!myNetData.hasRemaining()) {
			myNetData.clear();
			fillAppData();
			myNetData.flip();
		}

		System.out.println(myNetData.remaining());

		if(getSocket().write(myNetData) == -1) {
			close();
			return;
		}

		if(!myAppData.hasRemaining() && !myNetData.hasRemaining()) {
			finishWrite();
		}
	}

	protected void fillAppData() throws IOException {
		if(!myAppData.hasRemaining()) {
			myAppData.clear();
			writeData(myAppData);
			myAppData.flip();
		}

		System.out.println("FILLING: " + myAppData.remaining());
		System.out.println(new String(myAppData.array()));
		SSLEngineResult res = engine.wrap(myAppData, myNetData);
		handleHandshakeStatus(res.getStatus());
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
			case FINISHED:
			case NOT_HANDSHAKING:
				key.interestOps(SelectionKey.OP_READ);
				break;
			default:
				break;
		}

		if(myNetData.hasRemaining()) key.interestOpsOr(SelectionKey.OP_WRITE);
	}

	protected void performHandshake(boolean canRead, boolean canWrite) throws IOException {
		if (canWrite && myNetData.hasRemaining()) {
			if(getSocket().write(myNetData) == -1) {
				close();
				return;
			}

			return;
		}

		if (canRead && getSocket().read(peerNetData) == -1) {
			close();
			return;
		}

		while(engine.getHandshakeStatus() != HandshakeStatus.FINISHED
			&& engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
			System.out.println("S: " + engine.getHandshakeStatus());
			switch (engine.getHandshakeStatus()) {
				case FINISHED:
				case NOT_HANDSHAKING:
					convertPeerData();
					return;
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
				case NEED_UNWRAP_AGAIN: // Doesn't apply here
				default:
					break;
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
