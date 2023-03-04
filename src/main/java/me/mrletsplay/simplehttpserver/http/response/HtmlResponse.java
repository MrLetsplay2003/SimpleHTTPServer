package me.mrletsplay.simplehttpserver.http.response;

import java.nio.charset.StandardCharsets;

import me.mrletsplay.simplehttpserver.dom.html.HtmlDocument;
import me.mrletsplay.simplehttpserver.http.util.MimeType;

public class HtmlResponse implements HttpResponse {

	private HtmlDocument html;

	public HtmlResponse(HtmlDocument html) {
		this.html = html;
	}

	@Override
	public byte[] getContent() {
		return html.toString().getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public MimeType getContentType() {
		return MimeType.HTML;
	}

}
