package me.mrletsplay.simplehttpserver.reader;

import java.util.List;

import me.mrletsplay.simplehttpserver.util.UnsafeConsumer;
import me.mrletsplay.simplehttpserver.util.UnsafeFunction;
import me.mrletsplay.simplehttpserver.util.UnsafeRunnable;

public interface Reader<T> {

	public Ref<Byte> readByte();

	public Ref<Integer> readInt();

	public Ref<byte[]> readNBytes(int n);

	public Ref<byte[]> readNBytes(Ref<Integer> n);

	public Ref<byte[]> readUntilByte(byte delimiter, int limit);

	public Ref<byte[]> readUntilByte(Ref<Byte> delimiter, int limit);

	public Ref<byte[]> readUntilBytes(byte[] delimiter, int limit);

	public Ref<byte[]> readUntilBytes(Ref<byte[]> delimiter, int limit);

	public void read(Operation operation);

	public <O> Ref<O> read(Reader<O> reader);

	public void branch(Ref<Boolean> condition, Operation ifTrue, Operation ifFalse);

	public void branch(Ref<Integer> value, Operation... branches);

	public <O> Ref<O> branch(Ref<Boolean> condition, Reader<? extends O> ifTrue, Reader<? extends O> ifFalse);

	public <O> Ref<O> branch(Ref<Integer> value, @SuppressWarnings("unchecked") Reader<? extends O>... branches);

	public void loopUntil(Ref<Boolean> condition, Operation body);

	public <O> Ref<List<O>> loopUntil(Ref<Boolean> condition, Reader<O> body);

	public void run(UnsafeRunnable action);

	public void run(UnsafeConsumer<ReaderInstance<?>> action);

	public Expectation expect(Ref<Boolean> condition);

	public Expectation expectByte(byte b);

	public Expectation expectByte(Ref<Byte> b);

	public Expectation expectBytes(byte[] bytes);

	public Expectation expectBytes(Ref<byte[]> b);

	public void setConverter(UnsafeFunction<ReaderInstance<T>, T> converter);

	public ReaderInstance<T> createInstance();

}
