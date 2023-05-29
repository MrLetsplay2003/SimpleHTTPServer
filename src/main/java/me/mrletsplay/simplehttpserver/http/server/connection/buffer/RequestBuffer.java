package me.mrletsplay.simplehttpserver.http.server.connection.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import me.mrletsplay.simplehttpserver.http.exception.HttpBadRequestException;
import me.mrletsplay.simplehttpserver.http.header.HttpClientHeader;

public class RequestBuffer {

	private static final byte[] END_OF_HEADER = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);

	private HttpClientHeader header;
	private boolean complete;

	private int headerEndIndex(ByteBuffer buffer) {
		byte[] arr = buffer.array();
		o: for(int i = buffer.position(); i <= buffer.position() + buffer.remaining() - END_OF_HEADER.length; i++) {
			for(int j = 0; j < END_OF_HEADER.length; j++) {
				if(arr[i + j] != END_OF_HEADER[j]) continue o;
			}

			return i + END_OF_HEADER.length;
		}

		return -1;
	}

	public void readData(ByteBuffer buffer) throws IOException {
		if(header == null) {
			int endIndex;
			if((endIndex = headerEndIndex(buffer)) == -1) return;

			header = HttpClientHeader.parseHead(buffer.array(), 0, endIndex);
			buffer.position(endIndex);

			if(header == null) throw new HttpBadRequestException("Malformed request");
		}else {
			header.readBody(buffer);
		}

		if(header.isBodyComplete()) complete = true;
	}

	public boolean isComplete() {
		return complete;
	}

	public HttpClientHeader getHeader() {
		return header;
	}

	public void clear() {
		header = null;
		complete = false;
	}

}
