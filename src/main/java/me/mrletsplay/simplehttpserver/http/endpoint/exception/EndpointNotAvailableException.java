package me.mrletsplay.simplehttpserver.http.endpoint.exception;

public class EndpointNotAvailableException extends EndpointException {

	private static final long serialVersionUID = 8103555221879681715L;

	public EndpointNotAvailableException() {
		super();
	}

	public EndpointNotAvailableException(String message, Throwable cause) {
		super(message, cause);
	}

	public EndpointNotAvailableException(String message) {
		super(message);
	}

	public EndpointNotAvailableException(Throwable cause) {
		super(cause);
	}

}
