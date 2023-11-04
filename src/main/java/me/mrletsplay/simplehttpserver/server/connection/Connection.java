package me.mrletsplay.simplehttpserver.server.connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import me.mrletsplay.simplehttpserver.server.Server;
import me.mrletsplay.simplehttpserver.server.ServerException;

public interface Connection {

	public default boolean canReadData() {
		return getSelectionKey().isReadable();
	}

	public default boolean canWriteData() {
		return getSelectionKey().isWritable();
	}

	public void readData() throws IOException;

	public void writeData() throws IOException;

	public Server getServer();

	public SocketChannel getSocket();

	public SelectionKey getSelectionKey();

	public void setDead();

	public boolean isDead();

	public default void close() {
		try {
			getSelectionKey().cancel();
			getSocket().close();
			getServer().getConnectionAcceptor().remove(this);
		} catch (IOException e) {
			throw new ServerException("Error while closing the connection", e);
		}
	}

	public default boolean isSocketAlive() {
		return !isDead() && getSocket().isConnected() || getSocket().isConnectionPending();
	}

}
