package me.mrletsplay.simplehttpserver.http.endpoint.exception;

public class EndpointException extends RuntimeException {

	private static final long serialVersionUID = -941691822678857388L;

	public EndpointException() {
		super();
	}

	public EndpointException(String message, Throwable cause) {
		super(message, cause);
	}

	public EndpointException(String message) {
		super(message);
	}

	public EndpointException(Throwable cause) {
		super(cause);
	}

}
