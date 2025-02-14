package me.mrletsplay.simplehttpserver.http.response;

import java.nio.charset.StandardCharsets;

import me.mrletsplay.simplehttpserver.dom.js.JSScript;
import me.mrletsplay.simplehttpserver.http.util.MimeType;

public class JSResponse extends FixedHttpResponse {

	private JSScript js;

	public JSResponse(JSScript js) {
		this.js = js;
	}

	@Override
	public byte[] getContentBytes() {
		return js.toString().getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public MimeType getContentType() {
		return MimeType.JAVASCRIPT;
	}

}
