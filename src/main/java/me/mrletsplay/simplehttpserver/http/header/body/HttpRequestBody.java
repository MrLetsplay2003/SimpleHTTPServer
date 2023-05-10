package me.mrletsplay.simplehttpserver.http.header.body;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class HttpRequestBody {

	protected boolean complete;

	public abstract void read(ByteBuffer buffer) throws IOException;

	public abstract byte[] toByteArray();

	public boolean isComplete() {
		return complete;
	}

}
