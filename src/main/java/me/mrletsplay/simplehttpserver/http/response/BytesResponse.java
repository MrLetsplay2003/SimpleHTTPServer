package me.mrletsplay.simplehttpserver.http.response;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.simplehttpserver.http.util.MimeType;

public class BytesResponse extends FixedHttpResponse {

	private MimeType mimeType;
	private InputStream in;

	public BytesResponse(MimeType mimeType, InputStream in) {
		this.in = in;
	}

	public BytesResponse(MimeType mimeType, byte[] bytes) {
		this.in = new ByteArrayInputStream(bytes);
	}

	@Override
	public byte[] getContentBytes() {
		try {
			return in.readAllBytes();
		} catch (IOException e) {
			throw new FriendlyException(e);
		}
	}

	@Override
	public MimeType getContentType() {
		return mimeType;
	}

}
