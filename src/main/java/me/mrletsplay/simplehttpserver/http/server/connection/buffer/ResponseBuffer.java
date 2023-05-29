package me.mrletsplay.simplehttpserver.http.server.connection.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import me.mrletsplay.simplehttpserver.http.header.HttpServerHeader;

public class ResponseBuffer {

	private static final int BUFFER_SIZE = 4096;

	private ByteBuffer buffer;

	private ByteBuffer headerBuffer;
	private InputStream bodyStream;
	private HttpServerHeader response;

	private boolean complete;

	public ResponseBuffer() {
		this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
		this.complete = true;
	}

	public void init(HttpServerHeader response) throws IOException {
		if(this.response != null) throw new IllegalStateException("Buffer is already initialized and must be cleared first");

		this.response = response;
		this.bodyStream = response.getContent();
		this.bodyStream.skip(response.getContentOffset());
		this.complete = false;
	}

	private boolean fillBuffer() throws IOException {
		buffer.clear();

		int n;
		if((n = bodyStream.read(buffer.array(), buffer.position(), buffer.limit())) == -1) { // TODO: prevent blocking by checking available() first
			return false;
		}
		buffer.position(n);

		buffer.flip();
		return true;
	}

	public void writeData(ByteChannel channel) throws IOException {
		if(headerBuffer == null) {
			headerBuffer = ByteBuffer.wrap(response.getHeaderBytes());
		}else if(headerBuffer.remaining() > 0) {
			channel.write(headerBuffer);

			if(headerBuffer.remaining() == 0) {
				if(!fillBuffer()) complete = true;
			}
		}else {
			if(buffer.remaining() == 0 && !fillBuffer()) {
				complete = true;
			}

			channel.write(buffer);
		}
	}

	public boolean isComplete() {
		return complete;
	}

	public void clear() {
		buffer.clear();
		headerBuffer = null;
		bodyStream = null;
		response = null;
		complete = true;
	}

}
