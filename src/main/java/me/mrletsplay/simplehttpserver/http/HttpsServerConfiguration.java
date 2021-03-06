package me.mrletsplay.simplehttpserver.http;

import java.io.File;

import org.slf4j.Logger;

public class HttpsServerConfiguration extends HttpServerConfiguration {

	private File
		certificateFile,
		certificateKeyFile;

	private String certificatePassword;

	protected HttpsServerConfiguration(String host, int port, Logger logger, boolean debugMode, File certificateFile, File certificateKeyFile, String certificatePassword) {
		super(host, port, logger, debugMode);
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
			return new HttpsServerConfiguration(host, port, logger, debugMode, certificateFile, certificateKeyFile, certificatePassword);
		}

	}

}
