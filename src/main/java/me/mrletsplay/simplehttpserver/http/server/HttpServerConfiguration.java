package me.mrletsplay.simplehttpserver.http.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.mrletsplay.simplehttpserver.server.impl.AbstractServerConfiguration;

public class HttpServerConfiguration extends AbstractServerConfiguration {

	protected boolean debugMode;

	protected HttpServerConfiguration(String host, int port, Logger logger, int poolSize, boolean debugMode) {
		super(host, port, logger, poolSize);
		this.debugMode = debugMode;
	}

	public boolean isDebugMode() {
		return debugMode;
	}

	public static class Builder extends AbstractServerConfigurationBuilder<HttpServerConfiguration, Builder> {

		protected boolean debugMode;

		public Builder() {
			this.poolSize = Runtime.getRuntime().availableProcessors();
		}

		public Builder debugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		protected void verify() throws IllegalStateException {
			if(host == null) throw new IllegalStateException("Host must be set");
			if(port <= 0 || port > 65535) throw new IllegalStateException("Port must be set to a value between 1 and 65535");
			if(logger == null) logger = LoggerFactory.getLogger(HttpServer.class);
			if(poolSize <= 0) throw new IllegalStateException("Pool size must be set to a value greater than zero");
		}

		@Override
		public HttpServerConfiguration create() throws IllegalStateException {
			verify();
			return new HttpServerConfiguration(host, port, logger, poolSize, debugMode);
		}

	}

}
