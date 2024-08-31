package me.mrletsplay.simplehttpserver.http.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import me.mrletsplay.simplenio.reader.Operation;
import me.mrletsplay.simplenio.reader.ReaderInstance;
import me.mrletsplay.simplenio.reader.SimpleRef;

class ReadLineOperation implements Operation {

	private static final byte[] DELIMITER = "\r\n".getBytes(StandardCharsets.UTF_8);

	private SimpleRef<byte[]> ref;
	private byte[] bytes;
	private int i;
	private boolean seenCR;

	public ReadLineOperation(SimpleRef<byte[]> ref, int limit) {
		this.ref = ref;
		this.bytes = new byte[limit];
		reset();
	}

	@Override
	public boolean read(ReaderInstance<?> instance, ByteBuffer buf) throws IOException {
		while(buf.hasRemaining() && i < bytes.length) {
			byte b = buf.get();
			if(seenCR && b != '\n') throw new IOException("Expected \\n after \\r");
			if(b == '\r') seenCR = true;
			bytes[i++] = b;

			if(i >= DELIMITER.length && Arrays.equals(bytes, i - DELIMITER.length, i, DELIMITER, 0, DELIMITER.length)) {
				ref.set(instance, Arrays.copyOfRange(bytes, 0, i));
				return true;
			}
		}

		if(i == bytes.length) throw new IOException("Buffer limit reached");

		return false;
	}

	@Override
	public Operation copy() {
		return new ReadLineOperation(ref, bytes.length);
	}

	@Override
	public void reset() {
		i = 0;
		seenCR = false;
	}

}