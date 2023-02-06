package me.mrletsplay.simplehttpserver.http.exception;

import me.mrletsplay.simplehttpserver.http.HttpStatusCodes;

public class HttpBadRequestException extends HttpResponseException {

	private static final long serialVersionUID = 7932097465126717816L;

	public HttpBadRequestException() {
		super(HttpStatusCodes.BAD_REQUEST_400);
	}

	public HttpBadRequestException(String message) {
		super(HttpStatusCodes.BAD_REQUEST_400, message);
	}

}
