package me.mrletsplay.simplehttpserver.http;

public enum HttpRequestMethod {

	GET,
	HEAD,
	POST,
	PUT,
	DELETE,
	CONNECT,
	OPTIONS,
	TRACE,
	PATCH,
	;

	public static HttpRequestMethod get(String raw) {
		try {
			return valueOf(raw.toUpperCase());
		}catch(IllegalArgumentException e) {
			return null;
		}
	}

}
