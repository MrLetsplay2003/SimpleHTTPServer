package me.mrletsplay.simplehttpserver.http.request;

import java.util.HashMap;
import java.util.Map;

import me.mrletsplay.simplehttpserver.http.HttpStatusCode;
import me.mrletsplay.simplehttpserver.http.HttpStatusCodes;
import me.mrletsplay.simplehttpserver.http.header.HttpClientContentType;
import me.mrletsplay.simplehttpserver.http.header.HttpClientHeader;
import me.mrletsplay.simplehttpserver.http.header.HttpServerHeader;
import me.mrletsplay.simplehttpserver.http.header.HttpUrlPath;
import me.mrletsplay.simplehttpserver.http.response.HttpResponse;
import me.mrletsplay.simplehttpserver.http.server.HttpServer;
import me.mrletsplay.simplehttpserver.http.server.connection.HttpConnection;

public class HttpRequestContext {

	private static ThreadLocal<HttpRequestContext> context = new ThreadLocal<>();

	private HttpConnection connection;
	private HttpClientHeader clientHeader;
	private HttpServerHeader serverHeader;
	private Map<String, String> pathParameters;
	private Map<String, Object> properties;
	private Exception exception;

	public HttpRequestContext(HttpConnection connection, HttpClientHeader clientHeader, HttpServerHeader serverHeader) {
		this.connection = connection;
		this.clientHeader = clientHeader;
		this.serverHeader = serverHeader;
		this.properties = new HashMap<>();
	}

	public HttpConnection getConnection() {
		return connection;
	}

	public HttpServer getServer() {
		return connection.getServer();
	}

	public HttpClientHeader getClientHeader() {
		return clientHeader;
	}

	public HttpUrlPath getRequestedPath() {
		return clientHeader.getPath();
	}

	public void setServerHeader(HttpServerHeader serverHeader) {
		this.serverHeader = serverHeader;
	}

	public HttpServerHeader getServerHeader() {
		return serverHeader;
	}

	public void setPathParameters(Map<String, String> pathParameters) {
		this.pathParameters = pathParameters;
	}

	public Map<String, String> getPathParameters() {
		return pathParameters;
	}

	public void setProperty(String name, Object value) {
		properties.put(name, value);
	}

	public Object getProperty(String name) {
		return properties.get(name);
	}

	public boolean isConnectionSecure() {
		return connection.isSecure();
	}

	public static void setCurrentContext(HttpRequestContext ctx) {
		context.set(ctx);
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	public Exception getException() {
		return exception;
	}

	public <T> T expectContent(HttpClientContentType<T> contentType) {
		try {
			return clientHeader.getPostData().getParsedAs(contentType);
		} catch(Exception e) {
			return null;
		}
	}

	public void redirect(HttpStatusCode code, String location) {
		serverHeader.setStatusCode(code);
		serverHeader.getFields().set("Location", location);
	}

	public void redirect(String location) {
		redirect(HttpStatusCodes.FOUND_302, location);
	}

	public void respond(HttpStatusCode statusCode, HttpResponse response) {
		serverHeader.setStatusCode(statusCode);
		serverHeader.setContent(response.getContentType(), response.getContent());
	}

	public static HttpRequestContext getCurrentContext() {
		HttpRequestContext ctx = context.get();
		if(ctx == null) throw new IllegalStateException("No context present");
		return ctx;
	}

}
