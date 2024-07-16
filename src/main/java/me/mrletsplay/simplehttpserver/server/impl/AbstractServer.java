package me.mrletsplay.simplehttpserver.server.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import me.mrletsplay.simplehttpserver.server.Server;
import me.mrletsplay.simplehttpserver.server.ServerException;
import me.mrletsplay.simplehttpserver.server.connection.Connection;
import me.mrletsplay.simplehttpserver.server.connection.ConnectionManager;

public abstract class AbstractServer implements Server {

	private AbstractServerConfiguration configuration;
	private ServerSocketChannel socket;
	private Selector[] selectors;
	private ConnectionManager manager;
	private ExecutorService executor;
	private int i = 0;

	public AbstractServer(AbstractServerConfiguration configuration) {
		this.configuration = configuration;
	}

	protected ServerSocketChannel createSocket() throws UnknownHostException, IOException {
		ServerSocketChannel socket = ServerSocketChannel.open();
		socket.bind(new InetSocketAddress(InetAddress.getByName(configuration.getHost()), configuration.getPort()));
		socket.configureBlocking(false);
		return socket;
	}

	protected abstract Connection createConnection(SelectionKey selectionKey, SocketChannel socket);

	@Override
	public void start() {
		try {
			selectors = new Selector[configuration.getIOWorkers()];
			for(int i = 0; i < configuration.getIOWorkers(); i++) {
				selectors[i] = Selector.open();
			}

			socket = createSocket();
			socket.register(selectors[0], SelectionKey.OP_ACCEPT);

			executor = Executors.newFixedThreadPool(configuration.getPoolSize());

			for(Selector selector : selectors) {
				executor.execute(() -> {
					while(!executor.isShutdown()) {
						try {
							workLoop(selector);
						}catch(Exception e) {
							getLogger().error("Error while accepting connection", e);
						}
					}
				});
			}
		} catch (IOException e) {
			throw new ServerException("Error while starting server", e);
		}
	}

	private void workLoop(Selector selector) throws IOException {
		selector.select(1000);
		if(executor.isShutdown()) return;

		Set<SelectionKey> selected = selector.selectedKeys();
		var iterator = selected.iterator();
		while(iterator.hasNext()) {
			SelectionKey key = iterator.next();

			if(key.isValid() && key.isAcceptable()) {
				acceptConnection();
			}

			if(key.attachment() instanceof Connection) {
				Connection con = (Connection) key.attachment();
				if(!con.isSocketAlive()) {
					con.close();
					continue;
				}

				if(key.isValid() && key.isReadable()) {
					try {
						con.readData();
					}catch(IOException e) {
						getLogger().error("Client read error", e);
						con.close();
					}
				}

				if(key.isValid() && key.isWritable()) {
					try {
						con.writeData();
					}catch(IOException e) {
						getLogger().error("Client write error", e);
						con.close();
					}
				}
			}

			iterator.remove();
		}
	}

	private void acceptConnection() throws IOException {
		SocketChannel client = socket.accept();
		client.configureBlocking(false);

		i = (i + 1) % selectors.length;
		SelectionKey key = client.register(selectors[i], SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		Connection con = createConnection(key, client); // TODO: handle error in createConnection, unregister
		key.attach(con);
		manager.accept(con);
	}

	@Override
	public boolean isRunning() {
		return socket != null && socket.isOpen();
	}

	@Override
	public void setConnectionManager(ConnectionManager acceptor) throws IllegalStateException {
		if(isRunning()) throw new IllegalStateException("Server is running");
		this.manager = acceptor;
	}

	@Override
	public ConnectionManager getConnectionManager() {
		return manager;
	}

	/**
	 * @see #getConfiguration()
	 * @return The configured host of this server
	 */
	@Deprecated
	@Override
	public String getHost() {
		return configuration.getHost();
	}

	/**
	 * @see #getConfiguration()
	 * @return The configured port of this server
	 */
	@Deprecated
	@Override
	public int getPort() {
		return configuration.getPort();
	}

	@Override
	public Logger getLogger() {
		return configuration.getLogger();
	}

	public AbstractServerConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public ExecutorService getExecutor() {
		return executor;
	}

	@Override
	public void shutdown() {
		try {
			executor.shutdown();
			for(Selector s : selectors) s.close();
			manager.closeAllConnections();
			try {
				if(!executor.awaitTermination(5L, TimeUnit.SECONDS)) executor.shutdownNow();
			}catch(InterruptedException e) {
				throw new ServerException("Error while stopping executor", e);
			}
			if(socket != null) socket.close();
		} catch (IOException e) {
			throw new ServerException("Error while stopping server", e);
		}
	}

}
