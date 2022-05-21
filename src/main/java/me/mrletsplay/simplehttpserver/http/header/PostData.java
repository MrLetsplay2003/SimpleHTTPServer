package me.mrletsplay.simplehttpserver.http.header;

import me.mrletsplay.simplehttpserver.server.ServerException;

public class PostData {

	private HttpHeaderFields headers;
	private byte[] content;

	public PostData(HttpHeaderFields headers, byte[] content) {
		this.headers = headers;
		this.content = content;
	}

	public String getContentType() {
		String contentType = headers.getFirst("Content-Type");
		contentType = (contentType != null && contentType.contains(";")) ? contentType.substring(0, contentType.indexOf(";")) : contentType;
		return contentType;
	}

	public <T> T getParsedAs(HttpClientContentType<T> type) {
		return type.parse(headers, content);
	}

	public Object getParsed() {
		String contentType = getContentType();
		DefaultClientContentTypes<?> t = DefaultClientContentTypes.getByMimeType(contentType);
		if(t == null) throw new ServerException("Unknown or unsupported content type: " + contentType);
		return getParsedAs(t);
	}

	public byte[] getRaw() {
		return content;
	}

}
