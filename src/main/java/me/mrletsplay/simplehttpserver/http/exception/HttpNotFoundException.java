package me.mrletsplay.simplehttpserver.http.exception;

import me.mrletsplay.simplehttpserver.http.HttpStatusCodes;

public class HttpNotFoundException extends HttpResponseException {

	private static final long serialVersionUID = 8908543741271312795L;

	public HttpNotFoundException() {
		super(HttpStatusCodes.NOT_FOUND_404);
	}

	public HttpNotFoundException(String message) {
		super(HttpStatusCodes.NOT_FOUND_404, message);
	}

}
