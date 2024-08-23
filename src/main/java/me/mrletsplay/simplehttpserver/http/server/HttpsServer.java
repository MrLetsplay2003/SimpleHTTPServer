package me.mrletsplay.simplehttpserver.http.server;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.simplehttpserver.http.server.connection.HttpsConnectionImpl;
import me.mrletsplay.simplehttpserver.http.util.SSLHelper;
import me.mrletsplay.simplehttpserver.server.connection.Connection;

public class HttpsServer extends HttpServer {

	private SSLContext sslContext;

	public HttpsServer(HttpsServerConfiguration configuration) {
		super(configuration);
		try {
			this.sslContext = SSLHelper.createSSLContext(configuration.getCertificateFile(), configuration.getCertificateKeyFile(), configuration.getCertificatePassword());
		} catch (IOException | GeneralSecurityException e) {
			throw new FriendlyException("Failed to intialize http server", e);
		}
	}

	@Deprecated
	public HttpsServer(String host, int port, File certificateFile, File keyFile, String certificatePassword) {
		this(newConfigurationBuilder()
				.host(host)
				.port(port)
				.certificate(certificateFile, keyFile)
				.certificatePassword(certificatePassword)
				.create());
	}

	@Deprecated
	public HttpsServer(int port, File certificateFile, File keyFile, String certificatePassword) {
		this(newConfigurationBuilder()
				.hostBindAll()
				.port(port)
				.certificate(certificateFile, keyFile)
				.certificatePassword(certificatePassword)
				.create());
	}

	@Override
	protected Connection createConnection(SelectionKey selectionKey, SocketChannel socket) {
		return new HttpsConnectionImpl(this, selectionKey, socket, sslContext);
	}

	public static HttpsServerConfiguration.Builder newConfigurationBuilder() {
		return new HttpsServerConfiguration.Builder();
	}

}
