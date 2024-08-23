package me.mrletsplay.simplehttpserver.server.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.mrletsplay.mrcore.misc.Builder;
import me.mrletsplay.simplehttpserver.http.server.HttpServer;

public class AbstractServerConfiguration {

	protected String host;
	protected int port;
	protected Logger logger;
	protected int poolSize;
	protected int ioWorkers;

	protected AbstractServerConfiguration(String host, int port, Logger logger, int poolSize, int ioWorkers) {
		this.host = host;
		this.port = port;
		this.logger = logger;
		this.poolSize = poolSize;
		this.ioWorkers = ioWorkers;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public Logger getLogger() {
		return logger;
	}

	public int getPoolSize() {
		return poolSize;
	}

	public int getIOWorkers() {
		return ioWorkers;
	}

	public static abstract class AbstractServerConfigurationBuilder<T extends AbstractServerConfiguration, Self extends Builder<T,Self>> implements Builder<T, Self>{

		protected String host;
		protected int port;
		protected Logger logger;
		protected int poolSize;
		protected int ioWorkers;

		protected AbstractServerConfigurationBuilder() {}

		public Self host(String host) {
			this.host = host;
			return getSelf();
		}

		/**
		 * This is equivalent to calling {@link #host(String) host("0.0.0.0")}
		 * @return This builder
		 */
		public Self hostBindAll() {
			this.host = "0.0.0.0";
			return getSelf();
		}

		public Self port(int port) {
			this.port = port;
			return getSelf();
		}

		public Self logger(Logger logger) {
			this.logger = logger;
			return getSelf();
		}

		public Self poolSize(int poolSize) {
			this.poolSize = poolSize;
			return getSelf();
		}

		public Self ioWorkers(int ioWorkers) {
			this.ioWorkers = ioWorkers;
			return getSelf();
		}

		protected void verify() {
			if(host == null) throw new IllegalStateException("Host must be set");
			if(port == 0) throw new IllegalStateException("Port must be set to a nonzero value");
			if(logger == null) logger = LoggerFactory.getLogger(HttpServer.class);
			if(poolSize <= 0) throw new IllegalStateException("Pool size must be greater than 0");
			if(ioWorkers <= 0) throw new IllegalStateException("Number of I/O workers must be greater than 0");
			if(poolSize <= ioWorkers) throw new IllegalStateException("Pool size must be greater than number of I/O workers");
		}

	}

}
