package me.mrletsplay.simplehttpserver.http.header.body;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

public class ChunkedRequestBody extends HttpRequestBody {

	private static final byte[] CRLF = "\r\n".getBytes(StandardCharsets.UTF_8);

	private ByteBuffer readBuffer = ByteBuffer.allocate(1024);
	private ByteArrayOutputStream data;

	private int remainingChunkSize = -1;

	public ChunkedRequestBody() {
		this.data = new ByteArrayOutputStream();
	}

	private int chunkSizeEndIndex() {
		byte[] arr = readBuffer.array();
		o: for(int i = 0; i <= readBuffer.remaining() - CRLF.length; i++) {
			for(int j = 0; j < CRLF.length; j++) {
				if(arr[i + j] != CRLF[j]) continue o;
			}

			return i + CRLF.length;
		}

		return -1;
	}

	private boolean check() {
		int endIdx = chunkSizeEndIndex();
		if(endIdx == -1) {
			if(!readBuffer.hasRemaining()) return false;
			return true;
		}

		String str = new String(readBuffer.array(), 0, endIdx - CRLF.length);
		try {
			remainingChunkSize = Integer.parseInt(str, 16);
			if(remainingChunkSize == 0) {
				// TODO: read trailing headers
				complete = true; // TODO: read remaining CRLFs?
				return true;
			}
		}catch(NumberFormatException e) {
			return false;
		}

		readBuffer.position(endIdx);
		readChunkData();

		return true;
	}

	private void readChunkData() {
		if(readBuffer.remaining() >= remainingChunkSize) {
			// Read remaining bytes
			data.write(readBuffer.array(), readBuffer.position(), remainingChunkSize);
			readBuffer.position(readBuffer.position() + remainingChunkSize);
			remainingChunkSize = -1;
		}else {
			data.write(readBuffer.array(), readBuffer.position(), readBuffer.remaining());
			readBuffer.position(readBuffer.position() + readBuffer.remaining());
		}
	}

	private void readData() {
		while(readBuffer.hasRemaining()) {
			if(remainingChunkSize == -1) {
				if(!check()) {
					// TODO: fail
					return;
				}

				if(complete) return;
			}

			if(remainingChunkSize != -1) {
				readChunkData();
			}
		}
	}

	@Override
	public void read(ByteBuffer buffer) throws IOException {
		if(complete) throw new IllegalStateException("Body is complete");
		if(!buffer.hasArray()) throw new IllegalArgumentException("Buffer must be backed by an array");

		readBuffer.put(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
		readBuffer.flip();
		readData();
	}

	@Override
	public void read(ReadableByteChannel channel) throws IOException {
		if(complete) throw new IllegalStateException("Body is complete");

		if(channel.read(readBuffer) == -1) return;

		readBuffer.flip();
		readData();
	}

	@Override
	public byte[] toByteArray() {
		return data.toByteArray();
	}

}
