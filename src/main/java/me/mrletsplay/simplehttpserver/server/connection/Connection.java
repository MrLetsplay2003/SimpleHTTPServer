package me.mrletsplay.simplehttpserver.server.connection;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import me.mrletsplay.simplehttpserver.server.Server;
import me.mrletsplay.simplehttpserver.server.ServerException;

public interface Connection {

	public void readData() throws IOException;

	public void writeData() throws IOException;

	public Server getServer();

	public SocketChannel getSocket();

	public default void close() {
		try {
			getSocket().close();
			getServer().getConnectionAcceptor().remove(this);
		} catch (IOException e) {
			throw new ServerException("Error while closing the connection", e);
		}
	}

	public default boolean isSocketAlive() {
		return getSocket().isConnected() || getSocket().isConnectionPending();
	}

}
