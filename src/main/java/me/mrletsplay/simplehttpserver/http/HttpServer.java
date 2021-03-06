package me.mrletsplay.simplehttpserver.http;

import java.util.ArrayList;
import java.util.List;

import me.mrletsplay.simplehttpserver.http.compression.DeflateCompression;
import me.mrletsplay.simplehttpserver.http.compression.GZIPCompression;
import me.mrletsplay.simplehttpserver.http.compression.HttpCompressionMethod;
import me.mrletsplay.simplehttpserver.http.document.DefaultDocumentProvider;
import me.mrletsplay.simplehttpserver.http.document.HttpDocumentProvider;
import me.mrletsplay.simplehttpserver.server.impl.AbstractServer;

public class HttpServer extends AbstractServer {

	private HttpDocumentProvider documentProvider;
	private HttpProtocolVersion protocolVersion;
	private List<HttpCompressionMethod> compressionMethods;

	public HttpServer(HttpServerConfiguration configuration) {
		super(configuration);
		this.protocolVersion = HttpProtocolVersions.HTTP1_1;
		this.compressionMethods = new ArrayList<>();
		setConnectionAcceptor(new HttpConnectionAcceptor(this));
		setDocumentProvider(new DefaultDocumentProvider());
		addCompressionMethod(new DeflateCompression());
		addCompressionMethod(new GZIPCompression());
	}

	@Deprecated
	public HttpServer(String host, int port) {
		this(newConfigurationBuilder()
				.host(host)
				.port(port)
				.create());
	}

	@Deprecated
	public HttpServer(int port) {
		this(newConfigurationBuilder()
				.hostBindAll()
				.port(port)
				.create());
	}

	@Override
	public HttpServerConfiguration getConfiguration() {
		return (HttpServerConfiguration) super.getConfiguration();
	}

	public void setDocumentProvider(HttpDocumentProvider documentProvider) throws IllegalStateException {
		if(isRunning()) throw new IllegalStateException("Server is running");
		this.documentProvider = documentProvider;
	}

	public HttpDocumentProvider getDocumentProvider() {
		return documentProvider;
	}

	public void addCompressionMethod(HttpCompressionMethod compressionMethod) {
		compressionMethods.add(compressionMethod);
	}

	public List<HttpCompressionMethod> getCompressionMethods() {
		return compressionMethods;
	}

	public HttpProtocolVersion getProtocolVersion() {
		return protocolVersion;
	}

	public static HttpServerConfiguration.Builder newConfigurationBuilder() {
		return new HttpServerConfiguration.Builder();
	}

}
