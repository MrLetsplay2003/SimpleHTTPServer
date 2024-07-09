package me.mrletsplay.simplehttpserver.server.impl;

import java.util.ArrayList;
import java.util.List;

import me.mrletsplay.simplehttpserver.server.Server;
import me.mrletsplay.simplehttpserver.server.connection.Connection;
import me.mrletsplay.simplehttpserver.server.connection.ConnectionManager;

public class ConnectionManagerImpl<S extends Server, C extends Connection> implements ConnectionManager {

	private S server;
	private List<C> connections;

	public ConnectionManagerImpl(S server) {
		this.server = server;
		this.connections = new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void accept(Connection connection) {
		C con = (C) connection;
		connections.add(con);
	}

	@Override
	public void remove(Connection connection) {
		connections.remove(connection);
	}

	@Override
	public List<C> getActiveConnections() {
		return connections;
	}

	@Override
	public S getServer() {
		return server;
	}

}
