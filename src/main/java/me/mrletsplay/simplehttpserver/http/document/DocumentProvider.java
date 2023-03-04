package me.mrletsplay.simplehttpserver.http.document;

import me.mrletsplay.simplehttpserver.http.HttpStatusCodes;
import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.util.PathMatcher;

public interface DocumentProvider {

	/**
	 * Adds a document at a given, fixed path to this document provider.<br>
	 * If a document with the given path already exists, it will be replaced
	 * @param method The request method of the document
	 * @param path The path of the document
	 * @param document The document to add
	 */
	public void register(HttpRequestMethod method, String path, HttpDocument document);

	/**
	 * Removes a document from this document provider
	 * @param method The request method of the document
	 * @param path The path of the document to remove
	 */
	public void unregister(HttpRequestMethod method, String path);

	/**
	 * Adds a document with a pattern-matched path to this document provider.<br>
	 * Make sure to not register multiple documents with overlapping paths, as it is not defined which of the documents will be used<br>
	 * <br>
	 * For more information about pattern matching, see {@link PathMatcher#match(String, String)}
	 * @param method The method of the document
	 * @param pattern The path pattern
	 * @param document The document to add
	 * @see PathMatcher#match(String, String)
	 */
	public void registerPattern(HttpRequestMethod method, String pattern, HttpDocument document);

	/**
	 * Removes a document with a pattern-matched path from this document provider
	 * @param method The method of the document
	 * @param pattern The path pattern of the document to remove
	 */
	public void unregisterPattern(HttpRequestMethod method, String pattern);

	/**
	 * Returns the document mapped to a specific path.<br>
	 * This includes both {@link #register(HttpRequestMethod, String, HttpDocument) fixed paths} and {@link #registerPattern(HttpRequestMethod, String, HttpDocument) pattern-matched paths}
	 * @param method
	 * @param path
	 * @return
	 */
	public HttpDocument get(HttpRequestMethod method, String path);

	/**
	 * @param document The document to use
	 * @see #getNotFoundDocument()
	 */
	public void setNotFoundDocument(HttpDocument document);

	/**
	 * Returns the document to use if an HTTP server cannot find a document with a given request method and path (typically results in a {@link HttpStatusCodes#NOT_FOUND_404 404 Not Found} response)
	 * @return The document to use
	 */
	public HttpDocument getNotFoundDocument();

	/**
	 * @param document The document to use
	 * @see #getErrorDocument()
	 */
	public void setErrorDocument(HttpDocument document);

	/**
	 * Returns the document to use if an HTTP server encounters an error (Java exception) while creating document content (typically results in a {@link HttpStatusCodes#INTERNAL_SERVER_ERROR_500 500 Internal Server Error} response).<br>
	 * The document may use the {@link HttpRequestContext#getException() request exception} in its response to provide further details
	 * @return The document to use
	 */
	public HttpDocument getErrorDocument();

}
