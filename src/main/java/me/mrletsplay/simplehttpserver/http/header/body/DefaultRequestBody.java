package me.mrletsplay.simplehttpserver.http.header.body;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class DefaultRequestBody extends HttpRequestBody {

	private int contentLength;
	private ByteArrayOutputStream data;

	public DefaultRequestBody(int contentLength) {
		this.contentLength = contentLength;
		this.data = new ByteArrayOutputStream();
	}

	@Override
	public void read(ByteBuffer buffer) throws IOException {
		if(complete) throw new IllegalStateException("Body is complete");
		if(!buffer.hasArray()) throw new IllegalArgumentException("Buffer must be backed by an array");

		int toRead = Math.min(buffer.remaining(), contentLength - data.size());
		data.write(buffer.array(), buffer.position(), toRead);
		buffer.position(buffer.position() + toRead);

		if(data.size() == contentLength) complete = true;
	}

	@Override
	public byte[] toByteArray() {
		return data.toByteArray();
	}

}
