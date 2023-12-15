package me.mrletsplay.simplehttpserver.http.server;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.mrletsplay.simplehttpserver.http.cors.CorsConfiguration;
import me.mrletsplay.simplehttpserver.server.impl.AbstractServerConfiguration;

public class HttpServerConfiguration extends AbstractServerConfiguration {

	protected boolean debugMode;
	protected CorsConfiguration defaultCorsConfiguration;
	protected long readTimeout;

	protected HttpServerConfiguration(String host, int port, Logger logger, boolean debugMode, CorsConfiguration defaultCorsConfiguration, long readTimeout) {
		super(host, port, logger);
		this.debugMode = debugMode;
		this.defaultCorsConfiguration = defaultCorsConfiguration;
		this.readTimeout = readTimeout;
	}

	public boolean isDebugMode() {
		return debugMode;
	}

	public CorsConfiguration getDefaultCorsConfiguration() {
		return defaultCorsConfiguration;
	}

	public long getReadTimeout() {
		return readTimeout;
	}

	public static class Builder extends AbstractServerConfigurationBuilder<HttpServerConfiguration, Builder> {

		protected boolean debugMode;
		protected CorsConfiguration defaultCorsConfiguration;
		protected long readTimeout;

		public Builder debugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder defaultCorsConfiguration(CorsConfiguration defaultCorsConfiguration) {
			this.defaultCorsConfiguration = defaultCorsConfiguration;
			return this;
		}

		public Builder readTimeout(long readTimeout) {
			this.readTimeout = readTimeout;
			return this;
		}

		public Builder readTimeout(long timeout, TimeUnit unit) {
			this.readTimeout = unit.toMillis(timeout);
			return this;
		}

		protected void verify() throws IllegalStateException {
			if(host == null) throw new IllegalStateException("Host must be set");
			if(port == 0) throw new IllegalStateException("Port must be set to a nonzero value");
			if(logger == null) logger = LoggerFactory.getLogger(HttpServer.class);
		}

		@Override
		public HttpServerConfiguration create() throws IllegalStateException {
			verify();
			return new HttpServerConfiguration(host, port, logger, debugMode, defaultCorsConfiguration, readTimeout);
		}

	}

}
