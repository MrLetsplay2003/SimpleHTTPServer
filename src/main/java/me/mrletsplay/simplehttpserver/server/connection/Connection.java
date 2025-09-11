package me.mrletsplay.simplehttpserver.server.connection;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;

import me.mrletsplay.simplehttpserver.server.Server;
import me.mrletsplay.simplehttpserver.server.ServerException;

public interface Connection {

	public void readData() throws IOException;

	public void writeData() throws IOException;

	public Server getServer();

	public SocketChannel getSocket();

	public SelectionKey getSelectionKey();

	public default Logger getLogger() {
		return getServer().getLogger();
	}

	public default void startReading() {
		if(!getSelectionKey().isValid() || isReadShutdown()) return;
		getLogger().trace("Start reading from client " + getSocket());
		getSelectionKey().interestOpsOr(SelectionKey.OP_READ);
		getSelectionKey().selector().wakeup();
	}

	public default void stopReading() {
		if(!getSelectionKey().isValid()) return;
		getLogger().trace("Stop reading from client " + getSocket());
		getSelectionKey().interestOpsAnd(~SelectionKey.OP_READ);
	}

	public default void startWriting() {
		if(!getSelectionKey().isValid() || isWriteShutdown()) return;
		getLogger().trace("Start writing to client " + getSocket());
		getSelectionKey().interestOpsOr(SelectionKey.OP_WRITE);
		getSelectionKey().selector().wakeup();
	}

	public default void stopWriting() {
		if(!getSelectionKey().isValid()) return;
		getLogger().trace("Stop writing to client " + getSocket());
		getSelectionKey().interestOpsAnd(~SelectionKey.OP_WRITE);
	}

	public void shutdownRead();

	public boolean isReadShutdown();

	public void shutdownWrite();

	public boolean isWriteShutdown();

	public default void close() {
		try {
			getLogger().debug("Closing connection");
			getSelectionKey().cancel();
			getSocket().close();
			getServer().getConnectionManager().remove(this);
		} catch (IOException e) {
			throw new ServerException("Error while closing the connection", e);
		}
	}

	public default boolean isSocketAlive() {
		return getSocket().isConnected() || getSocket().isConnectionPending();
	}

}
