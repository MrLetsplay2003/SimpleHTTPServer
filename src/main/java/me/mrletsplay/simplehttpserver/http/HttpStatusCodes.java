package me.mrletsplay.simplehttpserver.http;

public enum HttpStatusCodes implements HttpStatusCode {
	
	SWITCHING_PROTOCOLS_101(101, "Switching Protocols"),
	OK_200(200, "OK"),
	NO_CONTENT_204(204, "No Content"),
	PARTIAL_CONTENT_206(206, "Partial Content"),
	MOVED_PERMANENTLY_301(301, "Moved Permanently"),
	FOUND_302(302, "Found"),
	SEE_OTHER_303(303, "See Other"),
	TEMPORARY_REDIRECT_307(307, "Temporary Redirect"),
	BAD_REQUEST_400(400, "Bad Request"),
	UNAUTHORIZED_401(401, "Unauthorized"),
	ACCESS_DENIED_403(403, "Access Denied"),
	NOT_FOUND_404(404, "Not Found"),
	REQUESTED_RANGE_NOT_SATISFIABLE_416(416, "Requested Range Not Satisfiable"),
	INTERNAL_SERVER_ERROR_500(500, "Internal Server Error"),
	;
	
	private final int code;
	private final String msg;
	
	private HttpStatusCodes(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	@Override
	public int getStatusCode() {
		return code;
	}
	
	@Override
	public String getStatusMessage() {
		return msg;
	}

}
