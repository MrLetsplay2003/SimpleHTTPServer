package me.mrletsplay.simplehttpserver.http.response;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import me.mrletsplay.simplehttpserver.http.util.MimeType;

public class FileResponse implements HttpResponse {

	private MimeType mimeType;
	private Path filePath;

	public FileResponse(MimeType mimeType, Path filePath) {
		this.mimeType = mimeType;
		this.filePath = filePath;
	}

	public FileResponse(Path filePath) {
		this(null, filePath);
	}

	@Override
	public InputStream getContent() {
		return null;
	}

	@Override
	public long getContentLength() {
		try {
			return Files.size(filePath);
		} catch (IOException e) {
			return -1;
		}
	}

	@Override
	public MimeType getContentType() {
		return mimeType;
	}

}
