package me.mrletsplay.simplehttpserver.http.exception;

import me.mrletsplay.simplehttpserver.http.HttpStatusCode;

public class HttpResponseException extends RuntimeException {

	private static final long serialVersionUID = 8332577640801496230L;

	private HttpStatusCode statusCode;
	private String statusMessage;

	public HttpResponseException(HttpStatusCode statusCode, String statusMessage) {
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
	}

	public HttpResponseException(HttpStatusCode statusCode) {
		this(statusCode, null);
	}

	public HttpStatusCode getStatusCode() {
		return statusCode;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

}
