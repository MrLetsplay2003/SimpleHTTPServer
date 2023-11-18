package me.mrletsplay.simplehttpserver.http.request;

import me.mrletsplay.simplehttpserver.http.exception.HttpResponseException;
import me.mrletsplay.simplehttpserver.http.server.HttpServer;

public interface RequestProcessor {

	/**
	 * Processes a request<br>
	 * Note: Throwing an {@link HttpResponseException} is equivalent to responding to the request and then returning {@code false}
	 * @param context The request context
	 * @return Whether to continue processing the request (if used as a pre-processor)
	 * @see HttpServer#setRequestPreProcessor(RequestProcessor)
	 * @see HttpServer#setRequestPostProcessor(RequestProcessor)
	 */
	public boolean process(HttpRequestContext context) throws HttpResponseException;

}
