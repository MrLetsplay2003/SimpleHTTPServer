package me.mrletsplay.simplehttpserver.http.util;

public class MimeType {

	/**
	 * Common MIME types. Text-based MIME types (text, html, css, javascript, json) use the UTF-8 charset
	 */
	public static final MimeType
		UNKNOWN = of("application/unknown"),
		TEXT = of("text/plain", "utf-8"),
		HTML = of("text/html", "utf-8"),
		CSS = of("text/css", "utf-8"),
		JAVASCRIPT = of("text/javascript", "utf-8"),
		JSON = of("application/json", "utf-8"),
		PNG = of("image/png"),
		JPEG = of("image/jpeg"),
		SVG = of("image/svg+xml");

	private String mediaType;
	private String charset;
	private String boundary;

	private MimeType(String mediaType, String charset, String boundary) {
		this.mediaType = mediaType;
		this.charset = charset;
		this.boundary = boundary;
	}

	public String getMediaType() {
		return mediaType;
	}

	public String getCharset() {
		return charset;
	}

	public String getBoundary() {
		return boundary;
	}

	@Override
	public String toString() {
		StringBuilder type = new StringBuilder();
		type.append(mediaType);
		if(charset != null) type.append("; charset=").append(charset);
		if(boundary != null) type.append("; boundary=").append(boundary);
		return type.toString();
	}

	public static MimeType of(String mediaType) {
		return new MimeType(mediaType, null, null);
	}

	public static MimeType of(String mediaType, String charset) {
		return new MimeType(mediaType, charset, null);
	}

	public static MimeType multipartFormData(String boundary) {
		return new MimeType("multipart/form-data", null, boundary);
	}

	public static MimeType multipart(String mediaType, String boundary) {
		return new MimeType(mediaType, null, boundary);
	}

}
