package me.mrletsplay.simplehttpserver.http.server.connection.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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
		o: for(int i = 0; i <= buffer.position() - END_OF_HEADER.length; i++) {
			for(int j = 0; j < END_OF_HEADER.length; j++) {
				if(arr[i + j] != END_OF_HEADER[j]) continue o;
			}

			return i + END_OF_HEADER.length;
		}

		return -1;
	}

	public boolean readData() throws IOException {
		if(header == null) {
			if(connection.getSocket().read(buffer) == -1) return false;

			if(buffer.remaining() == 0) throw new IOException("Buffer is full");

			int endIndex;
			if((endIndex = headerEndIndex()) == -1) return true;

			buffer.flip();

			header = HttpClientHeader.parseHead(buffer.array(), 0, endIndex);
			buffer.position(endIndex);

			if(header == null) return false;

			if(!header.isBodyComplete()) header.readBody(buffer);

			buffer.clear();
		}else {
			header.readBody(connection.getSocket());
		}

		if(header.isBodyComplete()) complete = true;

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
