package me.mrletsplay.simplehttpserver.server.impl;

import org.slf4j.Logger;

import me.mrletsplay.mrcore.misc.Builder;

public class AbstractServerConfiguration {

	protected String host;
	protected int port;
	protected Logger logger;
	protected int poolSize;

	protected AbstractServerConfiguration(String host, int port, Logger logger, int poolSize) {
		this.host = host;
		this.port = port;
		this.logger = logger;
		this.poolSize = poolSize;
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

	public static abstract class AbstractServerConfigurationBuilder<T extends AbstractServerConfiguration, Self extends Builder<T,Self>> implements Builder<T, Self>{

		protected String host;
		protected int port;
		protected Logger logger;
		protected int poolSize;

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

	}

}
