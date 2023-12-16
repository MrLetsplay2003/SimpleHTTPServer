package me.mrletsplay.simplehttpserver.http.server;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.mrletsplay.simplehttpserver.http.cors.CorsConfiguration;
import me.mrletsplay.simplehttpserver.http.server.connection.HttpConnection;
import me.mrletsplay.simplehttpserver.server.impl.AbstractServerConfiguration;

public class HttpServerConfiguration extends AbstractServerConfiguration {

	protected boolean debugMode;
	protected boolean handleOptionsRequests;
	protected int maxClientHeaderSize;
	protected CorsConfiguration defaultCorsConfiguration;
	protected long readTimeout;

	protected HttpServerConfiguration(String host, int port, Logger logger, int poolSize, boolean debugMode, boolean handleOptionsRequests, int maxClientHeaderSize, CorsConfiguration defaultCorsConfiguration, long readTimeout) {
		super(host, port, logger, poolSize);
		this.debugMode = debugMode;
		this.handleOptionsRequests = handleOptionsRequests;
		this.maxClientHeaderSize = maxClientHeaderSize;
		this.defaultCorsConfiguration = defaultCorsConfiguration;
		this.readTimeout = readTimeout;
	}

	public boolean isDebugMode() {
		return debugMode;
	}

	public boolean isHandleOptionsRequests() {
		return handleOptionsRequests;
	}

	public int getMaxClientHeaderSize() {
		return maxClientHeaderSize;
	}

	public CorsConfiguration getDefaultCorsConfiguration() {
		return defaultCorsConfiguration;
	}

	public long getReadTimeout() {
		return readTimeout;
	}

	public static class Builder extends AbstractServerConfigurationBuilder<HttpServerConfiguration, Builder> {

		protected boolean debugMode;
		protected boolean handleOptionsRequests;
		protected int maxClientHeaderSize;
		protected CorsConfiguration defaultCorsConfiguration;
		protected long readTimeout;

		public Builder() {
			this.poolSize = Runtime.getRuntime().availableProcessors();
			this.handleOptionsRequests = true;
			this.maxClientHeaderSize = HttpConnection.DEFAULT_MAX_CLIENT_HEADER_SIZE;
		}

		public Builder debugMode(boolean debugMode) {
			this.debugMode = debugMode;
			return this;
		}

		public Builder handleOptionsRequests(boolean handleOptionsRequests) {
			this.handleOptionsRequests = handleOptionsRequests;
			return this;
		}

		public Builder maxClientHeaderSize(int maxClientHeaderSize) {
			this.maxClientHeaderSize = maxClientHeaderSize;
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
			if(port <= 0 || port > 65535) throw new IllegalStateException("Port must be set to a value between 1 and 65535");
			if(logger == null) logger = LoggerFactory.getLogger(HttpServer.class);
			if(poolSize <= 0) throw new IllegalStateException("Pool size must be set to a value greater than zero");
			if(maxClientHeaderSize <= 0) throw new IllegalStateException("Maximum client header size must be greater than zero");
		}

		@Override
		public HttpServerConfiguration create() throws IllegalStateException {
			verify();
			return new HttpServerConfiguration(host, port, logger, poolSize, debugMode, handleOptionsRequests, maxClientHeaderSize, defaultCorsConfiguration, readTimeout);
		}

	}

}
