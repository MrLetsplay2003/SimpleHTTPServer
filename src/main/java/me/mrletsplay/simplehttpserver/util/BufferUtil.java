package me.mrletsplay.simplehttpserver.util;

import java.nio.ByteBuffer;

public class BufferUtil {

	private BufferUtil() {}

	/**
	 * Copies bytes from {@code src} to {@code dst} until either the source buffer is empty or the destination buffer is full
	 * @param src Source buffer
	 * @param dst Destination buffer
	 * @throws IllegalArgumentException If a buffer is not backed by an array
	 */
	public static void copyAvailable(ByteBuffer src, ByteBuffer dst) throws IllegalArgumentException {
		if(!src.hasArray() || !dst.hasArray()) throw new IllegalArgumentException("Both buffers must be backed by an array");

		int toCopy = Math.min(src.remaining(), dst.remaining());
		System.arraycopy(src.array(), src.position() + src.arrayOffset(), dst.array(), dst.position() + dst.arrayOffset(), toCopy);
		src.position(src.position() + toCopy);
		dst.position(dst.position() + toCopy);
	}

	/**
	 * Reallocates the buffer to the specified size, possibly discarding data (if {@code newSize} is smaller than the data currently in the buffer)<br>
	 * The new buffer will retain the previous position/limit but have a capacity of {@code newSize}
	 * @param buffer Buffer
	 * @param newSize New size
	 * @return
	 */
	public static ByteBuffer reallocate(ByteBuffer buffer, int newSize) throws IllegalArgumentException {
		if(!buffer.hasArray()) throw new IllegalArgumentException("Buffer must be backed by an array");

		ByteBuffer newBuffer = ByteBuffer.allocate(newSize);
		int toCopy = Math.min(buffer.capacity(), newSize);
		System.arraycopy(buffer.array(), buffer.arrayOffset(), newBuffer.array(), newBuffer.arrayOffset(), toCopy);
		newBuffer.position(buffer.position());
		newBuffer.limit(buffer.limit());

		return newBuffer;
	}

}
