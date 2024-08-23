package me.mrletsplay.simplehttpserver.http.server.connection;

import me.mrletsplay.simplehttpserver.http.server.HttpServer;
import me.mrletsplay.simplehttpserver.server.impl.ConnectionManagerImpl;

public class HttpConnectionManager extends ConnectionManagerImpl<HttpServer, HttpConnection> {

	public HttpConnectionManager(HttpServer server) {
		super(server);
	}

}
