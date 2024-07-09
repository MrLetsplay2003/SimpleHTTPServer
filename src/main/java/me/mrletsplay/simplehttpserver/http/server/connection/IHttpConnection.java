package me.mrletsplay.simplehttpserver.http.server.connection;

import me.mrletsplay.simplehttpserver.server.connection.Connection;

public interface IHttpConnection extends Connection {

	public boolean isSecure();

}
