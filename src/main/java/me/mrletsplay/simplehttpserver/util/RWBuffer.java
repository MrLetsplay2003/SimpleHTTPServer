package me.mrletsplay.simplehttpserver.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * A wrapper for a {@link ByteBuffer} that includes semantics for a read and write mode.<br>
 * If the buffer is in the wrong mode, an exception will be thrown.
 */
public class RWBuffer {

	private boolean write;
	private ByteBuffer buffer;

	private RWBuffer(ByteBuffer buffer, boolean write) {
		this.buffer = buffer;
		this.write = write;
	}

	/**
	 * Asserts that a read operation can take place (the buffer must be in read mode)
	 * @return The buffer to read from
	 * @throws IllegalStateException If this buffer is in write mode
	 */
	public ByteBuffer read() throws IllegalStateException {
		if(write) throw new IllegalStateException("Trying to read in write mode");
		return buffer;
	}

	/**
	 * Asserts that a write operation can take place (the buffer must be in write mode)
	 * @return The buffer to write to
	 * @throws IllegalStateException If this buffer is in read mode
	 */
	public ByteBuffer write() throws IllegalStateException {
		if(!write) throw new IllegalStateException("Trying to write in read mode");
		return buffer;
	}

	/**
	 * Switches the mode from read to write mode and vice-versa. Also {@link ByteBuffer#compact() compacts} the buffer when switching to write mode.
	 */
	public void flip() {
		if(write) {
			buffer.flip();
		}else {
			buffer.compact();
		}

		write = !write;
	}

	public boolean hasRemaining() {
		return buffer.hasRemaining();
	}

	public int remaining() {
		return buffer.remaining();
	}

	public int capacity() {
		return buffer.capacity();
	}

	public void reallocate(int newSize) {
		buffer = BufferUtil.reallocate(buffer, newSize);
	}

	/**
	 * Returns a string representation of the contents of the buffer.<br>
	 * - If the buffer is in read mode, this corresponds to the remaining bytes that can be read from the buffer.<br>
	 * - If the buffer is in write mode, this corresponds to the bytes that have been written previously.
	 */
	@Override
	public String toString() {
		if(write) {
			return new String(buffer.array(), 0, buffer.position(), StandardCharsets.UTF_8);
		}else {
			return new String(buffer.array(), buffer.position(), buffer.remaining(), StandardCharsets.UTF_8);
		}
	}

	public static RWBuffer readableBuffer(int size) {
		ByteBuffer buf = ByteBuffer.allocate(size);
		buf.limit(0);
		return new RWBuffer(buf, false);
	}

	public static RWBuffer writableBuffer(int size) {
		ByteBuffer buf = ByteBuffer.allocate(size);
		return new RWBuffer(buf, true);
	}

}
