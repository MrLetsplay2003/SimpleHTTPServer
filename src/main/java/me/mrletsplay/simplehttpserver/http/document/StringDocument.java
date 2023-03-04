package me.mrletsplay.simplehttpserver.http.document;

import java.nio.charset.StandardCharsets;

import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.util.MimeType;

public class StringDocument implements HttpDocument {

	private String content;

	public StringDocument(String content) {
		this.content = content;
	}

	@Override
	public void createContent() {
		HttpRequestContext.getCurrentContext().getServerHeader().setContent(MimeType.TEXT, content.getBytes(StandardCharsets.UTF_8));
	}

}
