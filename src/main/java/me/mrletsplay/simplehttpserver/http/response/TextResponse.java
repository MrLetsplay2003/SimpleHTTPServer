package me.mrletsplay.simplehttpserver.http.response;

import java.nio.charset.StandardCharsets;

import me.mrletsplay.simplehttpserver.http.util.MimeType;

public class TextResponse extends FixedHttpResponse {

	private String text;

	public TextResponse(String text) {
		this.text = text;
	}

	@Override
	public byte[] getContentBytes() {
		return text.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public MimeType getContentType() {
		return MimeType.TEXT;
	}

}
