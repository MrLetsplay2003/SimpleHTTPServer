package me.mrletsplay.simplehttpserver.server;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import me.mrletsplay.simplehttpserver.server.connection.ConnectionAcceptor;
import me.mrletsplay.simplehttpserver.server.impl.AbstractServerConfiguration;

public interface Server {

	public void start();

	public boolean isRunning();

	public void setConnectionAcceptor(ConnectionAcceptor acceptor) throws IllegalStateException;

	public ConnectionAcceptor getConnectionAcceptor();

	public AbstractServerConfiguration getConfiguration();

	public void setExecutor(ExecutorService executor) throws IllegalStateException;

	public ExecutorService getExecutor();

	public Logger getLogger();

	public void shutdown();

}
