package me.mrletsplay.simplehttpserver.server.connection;

import java.net.Socket;
import java.util.Collection;

import me.mrletsplay.simplehttpserver.server.Server;
import me.mrletsplay.simplehttpserver.server.ServerException;

public interface ConnectionAcceptor {

	public Connection createConnection(Socket socket);

	public void accept(Connection connection);

	public void remove(Connection connection);

	public Collection<? extends Connection> getActiveConnections();

	public Server getServer();

	public default void closeAllConnections() {
		for(Connection c : getActiveConnections()) {
			try {
				c.close();
			} catch(ServerException e) {
				getServer().getLogger().debug("Error while closing connection", e);
			}
		}
	}

}
