package me.mrletsplay.simplehttpserver.http.header.body;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ChunkedRequestBody extends HttpRequestBody {

	private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);

	private ByteArrayOutputStream data;

	private int remainingChunkSize = -1;

	public ChunkedRequestBody() {
		this.data = new ByteArrayOutputStream();
	}

	private int chunkSizeEndIndex(ByteBuffer buffer) {
		byte[] arr = buffer.array();
		o: for(int i = buffer.position(); i <= buffer.position() + buffer.remaining() - CRLF.length; i++) {
			for(int j = 0; j < CRLF.length; j++) {
				if(arr[i + j] != CRLF[j]) continue o;
			}

			return i + CRLF.length;
		}

		return -1;
	}

	private boolean check(ByteBuffer buffer) {
		int endIdx = chunkSizeEndIndex(buffer);
		if(endIdx == -1) return true;

		String str = new String(buffer.array(), buffer.position(), endIdx - CRLF.length);
		try {
			remainingChunkSize = Integer.parseInt(str, 16);
			if(remainingChunkSize == 0) {
				// TODO: read trailing headers
				// FIXME: read \r\n
				complete = true;
				return true;
			}
		}catch(NumberFormatException e) {
			return false;
		}

		buffer.position(endIdx);
		readChunkData(buffer);

		return true;
	}

	private void readChunkData(ByteBuffer buffer) {
		int toRead = Math.min(remainingChunkSize, buffer.remaining());
		data.write(buffer.array(), buffer.position(), toRead);
		buffer.position(buffer.position() + toRead);
		remainingChunkSize -= toRead;
		if(remainingChunkSize == -1) remainingChunkSize = -1;
	}

	@Override
	public void read(ByteBuffer buffer) throws IOException {
		if(complete) throw new IllegalStateException("Body is complete");
		if(!buffer.hasArray()) throw new IllegalArgumentException("Buffer must be backed by an array");

		while(buffer.hasRemaining()) {
			if(remainingChunkSize == -1) {
				if(!check(buffer)) {
					// TODO: fail
					return;
				}

				if(complete) return;
			}

			if(remainingChunkSize != -1) {
				readChunkData(buffer);
			}
		}
	}

//	@Override
//	public void read(ReadableByteChannel channel) throws IOException {
//		if(complete) throw new IllegalStateException("Body is complete");
//
//		if(channel.read(readBuffer) == -1) return;
//
//		readBuffer.flip();
//		readData();
//	}

	@Override
	public byte[] toByteArray() {
		return data.toByteArray();
	}

}
