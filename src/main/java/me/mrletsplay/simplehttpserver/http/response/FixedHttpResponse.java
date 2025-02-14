package me.mrletsplay.simplehttpserver.http.response;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public abstract class FixedHttpResponse implements HttpResponse {

	private byte[] bytes;

	public abstract byte[] getContentBytes();

	private void loadBytes() {
		if(bytes != null) return;
		bytes = getContentBytes();
	}

	@Override
	public InputStream getContent() {
		loadBytes();
		return new ByteArrayInputStream(bytes);
	}

	@Override
	public long getContentLength() {
		loadBytes();
		return bytes.length;
	}

}
