package me.mrletsplay.simplehttpserver.http.header.body;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public abstract class HttpRequestBody {

	protected boolean complete;

	public abstract void read(ByteBuffer buffer) throws IOException;

	public abstract void read(ReadableByteChannel channel) throws IOException;

	public abstract byte[] toByteArray();

	public boolean isComplete() {
		return complete;
	}

}
