package me.mrletsplay.simplehttpserver.server.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import me.mrletsplay.simplehttpserver.server.Server;

public abstract class AbstractConnection implements Connection {

	public static final int BUFFER_SIZE = 4096;

	private Server server;
	private SocketChannel socket;
	private boolean dead;

	private ByteBuffer
		readBuffer,
		writeBuffer;

	public AbstractConnection(Server server, SocketChannel socket) {
		this.server = server;
		this.socket = socket;
		this.readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		this.writeBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		writeBuffer.limit(0);
	}

	public abstract void readData(ByteBuffer buffer) throws IOException;

	public abstract void writeData(ByteBuffer buffer) throws IOException;

	@Override
	public void readData() throws IOException {
		if(readBuffer.remaining() == 0) throw new IOException("Read buffer overflow");
		if(getSocket().read(readBuffer) == -1) {
			close();
			return;
		}

		readBuffer.flip();
		readData(readBuffer);
		readBuffer.compact();
	}

	@Override
	public void writeData() throws IOException {
		if(writeBuffer.remaining() == 0) {
			writeBuffer.clear();
			writeData(writeBuffer);
			writeBuffer.flip();
		}

		if(getSocket().write(writeBuffer) == -1) {
			close();
			return;
		}
	}

	@Override
	public Server getServer() {
		return server;
	}

	@Override
	public SocketChannel getSocket() {
		return socket;
	}

	@Override
	public SelectionKey getSelectionKey() {
		return getSocket().keyFor(getServer().getSelector());
	}

	@Override
	public void setDead() {
		this.dead = true;
	}

	@Override
	public boolean isDead() {
		return dead;
	}

}
