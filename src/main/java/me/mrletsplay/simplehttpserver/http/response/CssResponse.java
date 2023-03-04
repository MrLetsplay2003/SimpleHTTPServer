package me.mrletsplay.simplehttpserver.http.response;

import java.nio.charset.StandardCharsets;

import javax.swing.text.html.StyleSheet;

import me.mrletsplay.simplehttpserver.http.util.MimeType;

public class CssResponse implements HttpResponse {

	private StyleSheet css;

	public CssResponse(StyleSheet css) {
		this.css = css;
	}

	@Override
	public byte[] getContent() {
		return css.toString().getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public MimeType getContentType() {
		return MimeType.CSS;
	}

}
