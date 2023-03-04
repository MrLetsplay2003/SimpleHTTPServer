package me.mrletsplay.simplehttpserver.http.response;

import me.mrletsplay.simplehttpserver.http.util.MimeType;

public interface HttpResponse {

	public byte[] getContent();

	public MimeType getContentType();

}
