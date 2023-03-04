package me.mrletsplay.simplehttpserver.http.document;

import java.util.Objects;

import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;

public class MethodAndPath {

	private HttpRequestMethod method;
	private String path;

	public MethodAndPath(HttpRequestMethod method, String path) {
		this.path = path;
		this.method = method;
	}

	public HttpRequestMethod getMethod() {
		return method;
	}

	public String getPath() {
		return path;
	}

	@Override
	public int hashCode() {
		return Objects.hash(method, path);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MethodAndPath other = (MethodAndPath) obj;
		return method == other.method && Objects.equals(path, other.path);
	}

	@Override
	public String toString() {
		return "[" + method + " " + path + "]";
	}

}
