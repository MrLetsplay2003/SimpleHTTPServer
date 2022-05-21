package me.mrletsplay.simplehttpserver.http.document;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import me.mrletsplay.simplehttpserver.http.util.PathMatcher;
import me.mrletsplay.simplehttpserver.php.PHP;
import me.mrletsplay.simplehttpserver.php.PHPFileDocument;

public interface HttpDocumentProvider {

	/**
	 * Adds a document at a given, fixed path to this document provider.<br>
	 * If a document with the given path already exists, it will be replaced
	 * @param path The path of the document
	 * @param document The document to add
	 */
	public void registerDocument(String path, HttpDocument document);

	/**
	 * Removes a document from this document provider
	 * @param path The path of the document to remove
	 */
	public void unregisterDocument(String path);

	/**
	 * Adds a document with a pattern-matched path to this document provider.<br>
	 * Make sure to not register multiple documents with overlapping paths, as it is not defined which of the documents will be used<br>
	 * <br>
	 * For more information about pattern matching, see {@link PathMatcher#match(String, String)}
	 * @param pattern The path pattern
	 * @param document The document to add
	 * @see PathMatcher#match(String, String)
	 */
	public void registerDocumentPattern(String pattern, HttpDocument document);

	/**
	 * Removes a document with a pattern-matched path from this document provider
	 * @param pattern The path pattern of the document to remove
	 */
	public void unregisterDocumentPattern(String pattern);

	public default void registerFileDocument(String path, File file) {
		if(file.isDirectory()) {
			for(File fl : file.listFiles()) {
				registerFileDocument(path + "/" + fl.getName(), fl);
			}
			return;
		}
		registerDocument(path, createFileDocument(file));
	}

	public default HttpDocument createFileDocument(File file) {
		if(PHP.getFileExtensions().stream().anyMatch(file.getName()::endsWith)) {
			return new PHPFileDocument(file);
		}
		try {
			String mimeType = Files.probeContentType(file.toPath());
			return new FileDocument(file, mimeType);
		} catch (IOException e) {
			return new FileDocument(file);
		}
	}

	public HttpDocument getDocument(String path);

	public void set404Document(HttpDocument document);

	public HttpDocument get404Document();

	public void set500Document(HttpDocument document);

	public HttpDocument get500Document();

}
