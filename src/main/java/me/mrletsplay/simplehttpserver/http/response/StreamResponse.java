package me.mrletsplay.simplehttpserver.http.response;

import java.io.InputStream;

import me.mrletsplay.simplehttpserver.http.util.MimeType;

public class StreamResponse implements HttpResponse {

	private MimeType mimeType;
	private InputStream content;
	private long length;

	public StreamResponse(MimeType mimeType, InputStream content, long length) {
		this.mimeType = mimeType;
		this.content = content;
		this.length = length;
	}

	@Override
	public InputStream getContent() {
		return content;
	}

	@Override
	public long getContentLength() {
		return length;
	}

	@Override
	public MimeType getContentType() {
		return mimeType;
	}

}
