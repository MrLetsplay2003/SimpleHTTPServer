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
import me.mrletsplay.simplehttpserver.server.connection.ConnectionAcceptor;

public abstract class AbstractServer implements Server {

	private AbstractServerConfiguration configuration;
	private Selector selector;
	private ServerSocketChannel socket;
	private ConnectionAcceptor acceptor;
	private ExecutorService executor;

	public AbstractServer(AbstractServerConfiguration configuration) {
		this.configuration = configuration;
	}

	protected ServerSocketChannel createSocket() throws UnknownHostException, IOException {
		ServerSocketChannel socket = ServerSocketChannel.open();
		socket.bind(new InetSocketAddress(InetAddress.getByName(configuration.getHost()), configuration.getPort()));
		socket.configureBlocking(false);
		return socket;
	}

	@Override
	public void start() {
		try {
			selector = Selector.open();
			socket = createSocket();
			socket.register(selector, SelectionKey.OP_ACCEPT);

			executor = Executors.newFixedThreadPool(configuration.getPoolSize());
			executor.execute(() -> {
				while(!executor.isShutdown()) {
					try {
						workLoop();
					}catch(Exception e) {
						getLogger().error("Error in work loop", e);
					}
				}
			});
		} catch (IOException e) {
			throw new ServerException("Error while starting server", e);
		}
	}

	private void workLoop() throws IOException {
		selector.select(1000);
		if(executor.isShutdown()) return;

		Set<SelectionKey> selected = selector.selectedKeys();
		var iterator = selected.iterator();
		while(iterator.hasNext()) {
			SelectionKey key = iterator.next();

			if(key.isAcceptable()) {
				Connection con = acceptConnection(key);
				con.getSocket().register(selector, SelectionKey.OP_READ, con);
			}

			if(key.isReadable()) {
				Connection con = (Connection) key.attachment();
				try {
					if(!con.isSocketAlive() || !con.readData()) {
						con.close();
						continue;
					}
				}catch(IOException e) {
					getLogger().error("Client read error", e);
				}
			}

			if(key.isWritable()) {
				Connection con = (Connection) key.attachment();
				try {
					if(!con.isSocketAlive() || !con.writeData()) {
						con.close();
						continue;
					}
				}catch(IOException e) {
					getLogger().error("Client write error", e);
				}
			}

			iterator.remove();
		}
	}

	private Connection acceptConnection(SelectionKey key) throws IOException {
		SocketChannel client = socket.accept();
		client.configureBlocking(false);

		Connection con = acceptor.createConnection(client);
		acceptor.accept(con);
		return con;
	}

	@Override
	public boolean isRunning() {
		return socket != null && socket.isOpen();
	}

	@Override
	public void setConnectionAcceptor(ConnectionAcceptor acceptor) throws IllegalStateException {
		if(isRunning()) throw new IllegalStateException("Server is running");
		this.acceptor = acceptor;
	}

	@Override
	public ConnectionAcceptor getConnectionAcceptor() {
		return acceptor;
	}

	@Override
	public Logger getLogger() {
		return configuration.getLogger();
	}

	@Override
	public AbstractServerConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public void setExecutor(ExecutorService executor) throws IllegalStateException {
		if(isRunning()) throw new IllegalStateException("Server is running");
		this.executor = executor;
	}

	@Override
	public ExecutorService getExecutor() {
		return executor;
	}

	@Override
	public Selector getSelector() {
		return selector;
	}

	@Override
	public void shutdown() {
		try {
			executor.shutdown();
			acceptor.closeAllConnections();
			try {
				if(!executor.awaitTermination(5L, TimeUnit.SECONDS)) executor.shutdownNow();
			}catch(InterruptedException e) {
				throw new ServerException("Error while stopping executor", e);
			}
			if(selector != null) selector.close();
			if(socket != null) socket.close();
		} catch (IOException e) {
			throw new ServerException("Error while stopping server", e);
		}
	}

}
