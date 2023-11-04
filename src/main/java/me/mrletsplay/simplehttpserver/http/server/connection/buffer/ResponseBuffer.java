package me.mrletsplay.simplehttpserver.http.server.connection.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import me.mrletsplay.simplehttpserver.http.header.HttpServerHeader;
import me.mrletsplay.simplehttpserver.util.BufferUtil;

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

	public void writeData(ByteBuffer buffer) throws IOException {
		System.out.println("WD");
		if(headerBuffer == null) {
			headerBuffer = ByteBuffer.wrap(response.getHeaderBytes());
		}

		if(headerBuffer.remaining() > 0) {
			BufferUtil.copyAvailable(headerBuffer, buffer);

			if(!headerBuffer.hasRemaining()) {
				if(!fillBuffer()) complete = true;
			}
		}else {
			if(!this.buffer.hasRemaining() && !fillBuffer()) {
				complete = true;
			}

			BufferUtil.copyAvailable(this.buffer, buffer);
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
