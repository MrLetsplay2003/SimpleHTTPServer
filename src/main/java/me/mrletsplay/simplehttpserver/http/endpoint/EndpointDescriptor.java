package me.mrletsplay.simplehttpserver.http.endpoint;

import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;

public class EndpointDescriptor {

	private HttpRequestMethod method;
	private String path;
	private boolean pattern;

	public EndpointDescriptor(HttpRequestMethod method, String path, boolean pattern) {
		this.method = method;
		this.path = path;
		this.pattern = pattern;
	}

	public HttpRequestMethod getMethod() {
		return method;
	}

	public String getPath() {
		return path;
	}

	public boolean isPattern() {
		return pattern;
	}

	@Override
	public String toString() {
		return "EndpointDescriptor [method=" + method + ", path=" + path + ", pattern=" + pattern + "]";
	}

}