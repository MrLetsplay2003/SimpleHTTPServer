package me.mrletsplay.simplehttpserver.http.header.body;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import me.mrletsplay.simplehttpserver.http.exception.HttpBadRequestException;

public class ChunkedRequestBody extends HttpRequestBody {

	private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);

	private ByteArrayOutputStream data;

	private int remainingChunkSize;
	private boolean crlf;
	private boolean finalChunk;

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
		if(endIdx == -1) return false;

		String str = new String(buffer.array(), buffer.position(), endIdx - buffer.position() - CRLF.length);
		buffer.position(endIdx);

		try {
			remainingChunkSize = Integer.parseInt(str, 16);
			crlf = true;
			if(remainingChunkSize == 0) {
				// TODO: read trailing headers
				finalChunk = true;
			}
		}catch(NumberFormatException e) {
			throw new HttpBadRequestException("Bad chunk size");
		}

		readChunkData(buffer);

		return true;
	}

	private void readChunkData(ByteBuffer buffer) {
		if(remainingChunkSize > 0) {
			int toRead = Math.min(remainingChunkSize, buffer.remaining());
			data.write(buffer.array(), buffer.position(), toRead);
			buffer.position(buffer.position() + toRead);
			remainingChunkSize -= toRead;
		}else if(crlf) {
			if(buffer.remaining() < CRLF.length) return;
			if(buffer.get() != '\r') throw new RuntimeException("Not CR");
			if(buffer.get() != '\n') throw new RuntimeException("Not LF");
			crlf = false;
			if(finalChunk) complete = true;
		}
	}

	@Override
	public void read(ByteBuffer buffer) throws IOException {
		if(complete) throw new IllegalStateException("Body is complete");
		if(!buffer.hasArray()) throw new IllegalArgumentException("Buffer must be backed by an array");

		if(remainingChunkSize == 0 && !crlf) {
			if(!check(buffer)) return;
			if(complete) return;
		}

		if(remainingChunkSize != 0 || crlf) {
			readChunkData(buffer);
		}
	}

	@Override
	public byte[] toByteArray() {
		return data.toByteArray();
	}

}
