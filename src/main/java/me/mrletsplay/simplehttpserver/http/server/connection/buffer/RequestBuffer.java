package me.mrletsplay.simplehttpserver.http.server.connection.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import me.mrletsplay.simplehttpserver.http.exception.HttpBadRequestException;
import me.mrletsplay.simplehttpserver.http.header.HttpClientHeader;
import me.mrletsplay.simplehttpserver.http.server.connection.HttpConnection;

public class RequestBuffer {

	public static final int MAX_CLIENT_HEADER_SIZE = 16384; // TODO: configurable
	private static final byte[] END_OF_HEADER = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);

	private HttpConnection connection;

	private ByteBuffer buffer;
	private HttpClientHeader header;
	private boolean complete;

	public RequestBuffer(HttpConnection connection) {
		this.buffer = ByteBuffer.allocate(MAX_CLIENT_HEADER_SIZE);
		this.connection = connection;
	}

	private int headerEndIndex() {
		byte[] arr = buffer.array();
		o: for(int i = buffer.position(); i <= buffer.position() + buffer.remaining() - END_OF_HEADER.length; i++) {
			for(int j = 0; j < END_OF_HEADER.length; j++) {
				if(arr[i + j] != END_OF_HEADER[j]) continue o;
			}

			return i + END_OF_HEADER.length;
		}

		return -1;
	}

	public boolean readData() throws IOException {
		if(buffer.remaining() == 0) throw new IOException("Buffer is full");

		if(connection.getSocket().read(buffer) == -1) return false;

		buffer.flip();

		while(buffer.hasRemaining()) {
			int before = buffer.position();

			if(header == null) {
				int endIndex;
				if((endIndex = headerEndIndex()) == -1) return true;

				header = HttpClientHeader.parseHead(buffer.array(), 0, endIndex);
				buffer.position(endIndex);

				if(header == null) throw new HttpBadRequestException("Malformed request");
			}else {
				header.readBody(buffer);
			}

			if(header.isBodyComplete()) {
				complete = true;
				break;
			}

			if(buffer.position() == before) break; // Nothing has been read this cycle, wait for more data from client
		}

		buffer.compact();

		return true;
	}

	public boolean isComplete() {
		return complete;
	}

	public HttpClientHeader getHeader() {
		return header;
	}

	public void clear() {
		complete = false;
		buffer.clear();
		header = null;
	}

}
