package me.mrletsplay.simplehttpserver.http.server;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import me.mrletsplay.simplehttpserver.http.cors.CorsConfiguration;

public class HttpsServerConfiguration extends HttpServerConfiguration {

	private File
		certificateFile,
		certificateKeyFile;

	private String certificatePassword;

	protected HttpsServerConfiguration(String host, int port, Logger logger, boolean debugMode, CorsConfiguration defaultCorsConfiguration, long readTimeout, File certificateFile, File certificateKeyFile, String certificatePassword) {
		super(host, port, logger, debugMode, defaultCorsConfiguration, readTimeout);
		this.certificateFile = certificateFile;
		this.certificateKeyFile = certificateKeyFile;
		this.certificatePassword = certificatePassword;
	}

	public File getCertificateFile() {
		return certificateFile;
	}

	public File getCertificateKeyFile() {
		return certificateKeyFile;
	}

	public String getCertificatePassword() {
		return certificatePassword;
	}

	public static class Builder extends HttpServerConfiguration.Builder {

		private File
			certificateFile,
			certificateKeyFile;

		private String certificatePassword;

		@Override
		public Builder host(String host) {
			return (Builder) super.host(host);
		}

		@Override
		public Builder hostBindAll() {
			return (Builder) super.hostBindAll();
		}

		@Override
		public Builder port(int port) {
			return (Builder) super.port(port);
		}

		@Override
		public Builder debugMode(boolean debugMode) {
			return (Builder) super.debugMode(debugMode);
		}

		@Override
		public Builder defaultCorsConfiguration(CorsConfiguration defaultCorsConfiguration) {
			return (Builder) super.defaultCorsConfiguration(defaultCorsConfiguration);
		}

		@Override
		public Builder readTimeout(long readTimeout) {
			return (Builder) super.readTimeout(readTimeout);
		}

		@Override
		public Builder readTimeout(long timeout, TimeUnit unit) {
			return (Builder) super.readTimeout(timeout, unit);
		}

		public Builder certificate(File certificateFile, File certificateKeyFile) {
			this.certificateFile = certificateFile;
			this.certificateKeyFile = certificateKeyFile;
			return this;
		}

		public Builder certificatePassword(String certificatePassword) {
			this.certificatePassword = certificatePassword;
			return this;
		}

		@Override
		protected void verify() throws IllegalStateException {
			super.verify();
			if(certificateFile == null) throw new IllegalStateException("Certificate file must be set");
			if(certificateKeyFile == null) throw new IllegalStateException("Certificate key file must be set");
		}

		@Override
		public HttpsServerConfiguration create() throws IllegalStateException {
			verify();
			return new HttpsServerConfiguration(host, port, logger, debugMode, defaultCorsConfiguration, readTimeout, certificateFile, certificateKeyFile, certificatePassword);
		}

	}

}
