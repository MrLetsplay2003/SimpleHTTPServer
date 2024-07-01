package me.mrletsplay.simplehttpserver.reader;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface OperationCallback {

	public boolean read(ReaderInstance<?> instance, ByteBuffer buf) throws IOException;

}
