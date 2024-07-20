package me.mrletsplay.simplehttpserver.reader;

import java.io.IOException;
import java.nio.ByteBuffer;

import me.mrletsplay.simplehttpserver.util.UnsafeConsumer;
import me.mrletsplay.simplehttpserver.util.UnsafeRunnable;

public interface Operation extends OperationCallback {

	@Override
	public boolean read(ReaderInstance<?> instance, ByteBuffer buf) throws IOException;

	public Operation copy();

	public void reset();

	public default Operation then(Operation other) {
		return Operations.allOf(this, other);
	}

	/**
	 * API Note: {@code other} must be a stateless runnable, because it might be called from multiple threads.
	 * @param other The action to run
	 * @return An Operation
	 */
	public default Operation thenRun(UnsafeRunnable other) {
		return then(Operations.stateless((instance, buf) -> {
			other.run();
			return true;
		}));
	}

	/**
	 * API Note: {@code other} must be a stateless runnable, because it might be called from multiple threads.
	 * @param other The action to run
	 * @return An Operation
	 */
	public default Operation thenRun(UnsafeConsumer<ReaderInstance<?>> other) {
		return then(Operations.stateless((instance, buf) -> {
			other.accept(instance);
			return true;
		}));
	}

}
