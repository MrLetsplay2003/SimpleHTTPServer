package me.mrletsplay.simplehttpserver.server.connection;

import java.io.IOException;
import java.net.Socket;

import me.mrletsplay.simplehttpserver.server.Server;
import me.mrletsplay.simplehttpserver.server.ServerException;

public interface Connection {

	public void startRecieving();

	public Server getServer();

	public Socket getSocket();

	public void setDead();

	public boolean isDead();

	public default void close() {
		try {
			getSocket().close();
			getServer().getConnectionAcceptor().remove(this);
		} catch (IOException e) {
			throw new ServerException("Error while closing the connection", e);
		}
	}

	public default boolean isSocketAlive() {
		return !isDead() && !getSocket().isClosed() && getSocket().isConnected() && getSocket().isBound() && !getSocket().isInputShutdown() && !getSocket().isOutputShutdown();
	}

}
