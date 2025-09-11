package me.mrletsplay.simplehttpserver.server.connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import me.mrletsplay.simplehttpserver.server.Server;

public abstract class AbstractConnection implements Connection {

	private Server server;
	private SelectionKey selectionKey;
	private SocketChannel socket;
	private boolean
		readShutdown,
		writeShutdown;

	public AbstractConnection(Server server, SelectionKey selectionKey, SocketChannel socket) {
		this.server = server;
		this.selectionKey = selectionKey;
		this.socket = socket;
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
		return selectionKey;
	}

	@Override
	public void shutdownRead() {
		getLogger().debug("Read shutdown for " + getSocket());
		this.readShutdown = true;
		stopReading();

		try {
			getSocket().shutdownInput();
		} catch(IOException e) {
			getLogger().debug("Failed to shutdown input", e);
		}
	}

	@Override
	public boolean isReadShutdown() {
		return readShutdown;
	}

	@Override
	public void shutdownWrite() {
		getLogger().debug("Write shutdown for " + getSocket());
		this.writeShutdown = true;
		stopWriting();

		try {
			getSocket().shutdownInput();
		} catch(IOException e) {
			getLogger().debug("Failed to shutdown output", e);
		}
	}

	@Override
	public boolean isWriteShutdown() {
		return writeShutdown;
	}

}
