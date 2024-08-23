package me.mrletsplay.simplehttpserver.http.server.connection;

import me.mrletsplay.simplehttpserver.http.server.HttpServer;
import me.mrletsplay.simplehttpserver.http.websocket.WebSocketConnection;
import me.mrletsplay.simplehttpserver.server.connection.Connection;

public interface HttpConnection extends Connection {

	@Override
	public HttpServer getServer();

	public boolean isSecure();

	public void setWebsocketConnection(WebSocketConnection connection);

	public WebSocketConnection getWebsocketConnection();

}
