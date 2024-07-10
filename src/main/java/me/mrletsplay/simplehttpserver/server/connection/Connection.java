package me.mrletsplay.simplehttpserver.server.connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import me.mrletsplay.simplehttpserver.server.Server;
import me.mrletsplay.simplehttpserver.server.ServerException;

public interface Connection {

	public void readData() throws IOException;

	public void writeData() throws IOException;

	public Server getServer();

	public SocketChannel getSocket();

	public SelectionKey getSelectionKey();

	public void setDead();

	public boolean isDead();

	public default void startReading() {
		getSelectionKey().interestOpsOr(SelectionKey.OP_READ);
		getServer().getSelector().wakeup();
	}

	public default void stopReading() {
		getSelectionKey().interestOpsAnd(~SelectionKey.OP_READ);
	}

	public default void startWriting() {
		getSelectionKey().interestOpsOr(SelectionKey.OP_WRITE);
		getServer().getSelector().wakeup();
	}

	public default void stopWriting() {
		getSelectionKey().interestOpsAnd(~SelectionKey.OP_WRITE);
	}

	public default void close() {
		try {
			getSelectionKey().cancel();
			getSocket().close();
			getServer().getConnectionManager().remove(this);
		} catch (IOException e) {
			throw new ServerException("Error while closing the connection", e);
		}
	}

	public default boolean isSocketAlive() {
		return !isDead() && getSocket().isConnected() || getSocket().isConnectionPending();
	}

}
