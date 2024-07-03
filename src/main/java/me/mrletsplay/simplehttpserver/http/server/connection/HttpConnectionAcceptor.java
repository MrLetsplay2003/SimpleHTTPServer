package me.mrletsplay.simplehttpserver.http.server.connection;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import me.mrletsplay.simplehttpserver.http.server.HttpServer;
import me.mrletsplay.simplehttpserver.server.connection.Connection;
import me.mrletsplay.simplehttpserver.server.connection.ConnectionAcceptor;

public class HttpConnectionAcceptor implements ConnectionAcceptor {

	private HttpServer server;
	private List<HttpConnection> connections;

	public HttpConnectionAcceptor(HttpServer server) {
		this.server = server;
		this.connections = new ArrayList<>();
	}

	@Override
	public HttpConnection createConnection(SocketChannel socket) {
		return new HttpConnection(server, socket);
	}

	@Override
	public void accept(Connection connection) {
		HttpConnection con = (HttpConnection) connection;
		connections.add(con);
	}

	@Override
	public void remove(Connection connection) {
		connections.remove(connection);
	}

	@Override
	public List<HttpConnection> getActiveConnections() {
		return connections;
	}

	@Override
	public HttpServer getServer() {
		return server;
	}

}
