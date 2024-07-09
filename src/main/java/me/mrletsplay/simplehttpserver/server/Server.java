package me.mrletsplay.simplehttpserver.server;

import java.nio.channels.Selector;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import me.mrletsplay.simplehttpserver.server.connection.ConnectionManager;

public interface Server {

	public void start();

	public boolean isRunning();

	public void setConnectionManager(ConnectionManager manager) throws IllegalStateException;

	public ConnectionManager getConnectionManager();

	public String getHost();

	public int getPort();

	public void setExecutor(ExecutorService executor) throws IllegalStateException;

	public ExecutorService getExecutor();

	public Logger getLogger();

	public Selector getSelector();

	public void shutdown();

}
