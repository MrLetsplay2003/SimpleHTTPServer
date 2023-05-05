package me.mrletsplay.simplehttpserver.http.header.body;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class DefaultRequestBody extends HttpRequestBody {

	private int contentLength;
	private ByteBuffer readBuffer = ByteBuffer.allocate(1024);
	private ByteArrayOutputStream data;

	public DefaultRequestBody(int contentLength) {
		this.contentLength = contentLength;
		this.data = new ByteArrayOutputStream();
	}

	@Override
	public void read(ByteBuffer buffer) throws IOException {
		if(complete) throw new IllegalStateException("Body is complete");

		readBuffer.flip();
		data.write(buffer.array(), buffer.position(), buffer.remaining());

		if(data.size() == contentLength) complete = true;
	}

	@Override
	public void read(ReadableByteChannel channel) throws IOException {
		if(complete) throw new IllegalStateException("Body is complete");

		if(channel.read(readBuffer) == -1) return;
		readBuffer.flip();
		data.write(readBuffer.array(), 0, readBuffer.remaining());

		if(data.size() == contentLength) complete = true;
	}

	@Override
	public byte[] toByteArray() {
		return data.toByteArray();
	}

}
