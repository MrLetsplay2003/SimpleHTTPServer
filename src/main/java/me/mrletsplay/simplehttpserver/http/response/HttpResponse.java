package me.mrletsplay.simplehttpserver.http.response;

import java.io.InputStream;

import me.mrletsplay.simplehttpserver.http.util.MimeType;

public interface HttpResponse {

	public InputStream getContent();

	public long getContentLength();

	public MimeType getContentType();

}
