package me.mrletsplay.simplehttpserver.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface ReaderInstance<T> {

	public boolean read(ByteBuffer buf) throws IOException;

	public ReaderInstance<T> onFinished(Consumer<T> consumer);

	public T get() throws IOException;

	public <R> void setRef(Ref<R> ref, R value) throws IllegalStateException;

	public <R> R getRef(Ref<R> ref) throws IllegalStateException;

	public void reset();

}
