package me.mrletsplay.simplehttpserver.server.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import me.mrletsplay.simplehttpserver.server.Server;

public abstract class AbstractBufferedConnection extends AbstractConnection {

	public static final int BUFFER_SIZE = 4096;

	private ByteBuffer
		readBuffer,
		writeBuffer;

	public AbstractBufferedConnection(Server server, SocketChannel socket) {
		super(server, socket);
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

}
