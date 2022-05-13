package me.mrletsplay.simplehttpserver.server.connection;

import java.net.Socket;

import me.mrletsplay.simplehttpserver.server.Server;

public abstract class AbstractConnection implements Connection {
	
	private Server server;
	private Socket socket;
	
	public AbstractConnection(Server server, Socket socket) {
		this.server = server;
		this.socket = socket;
	}

	@Override
	public Server getServer() {
		return server;
	}

	@Override
	public Socket getSocket() {
		return socket;
	}
	
}
