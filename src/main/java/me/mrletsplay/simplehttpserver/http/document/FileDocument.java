package me.mrletsplay.simplehttpserver.http.document;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.util.MimeType;
import me.mrletsplay.simplehttpserver.server.ServerException;

public class FileDocument implements HttpDocument {

	private Path path;
	private String mimeType;

	/**
	 * @deprecated Use {@link #FileDocument(Path, String)} instead
	 * @param file
	 * @param mimeType
	 */
	@Deprecated
	public FileDocument(File file, String mimeType) {
		this.path = file.toPath();
		this.mimeType = mimeType;
	}

	/**
	 * Does not do probing of content type!
	 * @deprecated Use {@link #FileDocument(Path)} instead
	 * @param file
	 */
	@Deprecated
	public FileDocument(File file) {
		this(file, null);
	}

	/**
	 * Creates a file document with the given MIME type
	 * @param path The path to the file
	 * @param mimeType The MIME type of the file
	 */
	public FileDocument(Path path, String mimeType) {
		this.path = path;
		this.mimeType = mimeType;
	}

	/**
	 * Creates a file document and tries to automatically detect the MIME type of the file
	 * @param path The path to the file
	 * @throws IOException If an IO error occurs while probing the file type
	 * @see Files#probeContentType(Path)
	 */
	public FileDocument(Path path) throws IOException {
		this(path, Files.probeContentType(path));
	}

	/**
	 * @deprecated Use {@link #getPath()} instead
	 * @return
	 */
	@Deprecated
	public File getFile() {
		return path.toFile();
	}

	/**
	 * @return The path to the file
	 */
	public Path getPath() {
		return path;
	}

	/**
	 * @return The MIME type of the file, or <code>null</code> for unknown type
	 */
	public String getMimeType() {
		return mimeType;
	}

	protected byte[] loadContent() {
		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			throw new ServerException("Failed to load file", e);
		}
	}

	@Override
	public void createContent() {
		HttpRequestContext.getCurrentContext().getServerHeader().setContent(MimeType.of(mimeType), loadContent());
	}

}
