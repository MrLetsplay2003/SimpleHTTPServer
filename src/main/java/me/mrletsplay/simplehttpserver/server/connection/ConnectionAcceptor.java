package me.mrletsplay.simplehttpserver.server.connection;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;

import me.mrletsplay.simplehttpserver.server.Server;
import me.mrletsplay.simplehttpserver.server.ServerException;

public interface ConnectionAcceptor {

	public Connection createConnection(SocketChannel socket);

	public void accept(Connection connection);

	public void remove(Connection connection);

	public Collection<? extends Connection> getActiveConnections();

	public Server getServer();

	public default void closeAllConnections() {
		for(Connection c : new ArrayList<>(getActiveConnections())) {
			try {
				c.close();
			} catch(ServerException e) {
				getServer().getLogger().error("Error while closing connection", e);
			}
		}
	}

}
