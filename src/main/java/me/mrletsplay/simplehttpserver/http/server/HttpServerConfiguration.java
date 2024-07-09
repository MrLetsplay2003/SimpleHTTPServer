package me.mrletsplay.simplehttpserver.http.server;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import me.mrletsplay.simplehttpserver.http.cors.CorsConfiguration;
import me.mrletsplay.simplehttpserver.server.impl.AbstractServerConfiguration;

public class HttpServerConfiguration extends AbstractServerConfiguration {

	protected boolean debugMode;
	protected CorsConfiguration defaultCorsConfiguration;
	protected long readTimeout;

	protected HttpServerConfiguration(String host, int port, Logger logger, int poolSize, boolean debugMode, CorsConfiguration defaultCorsConfiguration, long readTimeout) {
		super(host, port, logger, poolSize);
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

		@Override
		protected void verify() throws IllegalStateException {
			super.verify();
			if(readTimeout < 0) throw new IllegalStateException("Read timeout must be greater than or equal to 0");
		}

		@Override
		public HttpServerConfiguration create() throws IllegalStateException {
			verify();
			return new HttpServerConfiguration(host, port, logger, poolSize, debugMode, defaultCorsConfiguration, readTimeout);
		}

	}

}
