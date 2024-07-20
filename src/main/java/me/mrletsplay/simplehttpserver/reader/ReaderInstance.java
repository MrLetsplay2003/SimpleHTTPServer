package me.mrletsplay.simplehttpserver.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface ReaderInstance<T> {

	public boolean read(ByteBuffer buf) throws IOException;

	public ReaderInstance<T> onFinished(Consumer<T> consumer);

	public T get() throws IllegalStateException;

	public <R> void setRef(SimpleRef<R> ref, R value);

	public <R> R getRef(SimpleRef<R> ref) throws IllegalStateException;

	public boolean isRefSet(SimpleRef<?> ref);

	public void reset();

}
