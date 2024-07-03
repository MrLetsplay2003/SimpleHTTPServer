package me.mrletsplay.simplehttpserver.server.connection;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import me.mrletsplay.simplehttpserver.server.Server;

public abstract class AbstractConnection implements Connection {

	private Server server;
	private SocketChannel socket;
	private boolean dead;

	public AbstractConnection(Server server, SocketChannel socket) {
		this.server = server;
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
