package me.mrletsplay.simplehttpserver.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import me.mrletsplay.simplehttpserver.util.UnsafeConsumer;
import me.mrletsplay.simplehttpserver.util.UnsafeFunction;
import me.mrletsplay.simplehttpserver.util.UnsafeRunnable;

public class ReaderImpl<T> implements Reader<T> {

	private List<Operation> operations;
	private Map<Operation, StackTraceElement[]> stackTraces;
	private UnsafeFunction<ReaderInstance<T>, T> converter;

	public ReaderImpl() {
		this.operations = new ArrayList<>();
		this.stackTraces = new HashMap<>();
	}

	private void addOperation(Operation operation) {
		operations.add(operation);
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		stackTraces.put(operation, Arrays.copyOfRange(stackTrace, 1, stackTrace.length));
	}

	@Override
	public Ref<Byte> readByte() {
		SimpleRef<Byte> ref = SimpleRef.create();
		addOperation(Operations.readByte(ref));
		return ref;
	}

	@Override
	public Ref<Integer> readInt() {
		SimpleRef<Integer> valueRef = SimpleRef.create();
		addOperation(Operations.readInt(valueRef));
		return valueRef;
	}

	@Override
	public Ref<byte[]> readNBytes(int n) {
		SimpleRef<byte[]> ref = SimpleRef.create();
		addOperation(Operations.readNBytes(ref, n));
		return ref;
	}

	@Override
	public Ref<byte[]> readNBytes(Ref<Integer> n) {
		SimpleRef<byte[]> ref = SimpleRef.create();
		addOperation(Operations.lazy(instance -> Operations.readNBytes(ref, n.get(instance))));
		return ref;
	}

	@Override
	public Ref<byte[]> readUntilByte(byte delimiter, int limit) {
		SimpleRef<byte[]> ref = SimpleRef.create();
		addOperation(Operations.readUntilBytes(ref, new byte[] {delimiter}, limit));
		return ref;
	}

	@Override
	public Ref<byte[]> readUntilByte(Ref<Byte> delimiter, int limit) {
		SimpleRef<byte[]> ref = SimpleRef.create();
		addOperation(Operations.lazy(instance -> Operations.readUntilBytes(ref, new byte[] {delimiter.get(instance)}, limit)));
		return ref;
	}

	@Override
	public Ref<byte[]> readUntilBytes(byte[] delimiter, int limit) {
		SimpleRef<byte[]> ref = SimpleRef.create();
		addOperation(Operations.readUntilBytes(ref, delimiter, limit));
		return ref;
	}

	@Override
	public Ref<byte[]> readUntilBytes(Ref<byte[]> delimiter, int limit) {
		SimpleRef<byte[]> ref = SimpleRef.create();
		addOperation(Operations.lazy(instance -> Operations.readUntilBytes(ref, delimiter.get(instance), limit)));
		return ref;
	}

	@Override
	public void read(Operation operation) {
		addOperation(operation);
	}

	@Override
	public <O> Ref<O> read(Reader<O> reader) {
		SimpleRef<O> ref = SimpleRef.create();
		addOperation(Operations.read(ref, reader));
		return ref;
	}

	@Override
	public void branch(Ref<Boolean> condition, Operation ifTrue, Operation ifFalse) {
		addOperation(Operations.branch(condition.map(c -> c ? 0 : 1), ifTrue, ifFalse));
	}

	@Override
	public void branch(Ref<Integer> value, Operation... branches) {
		addOperation(Operations.branch(value, branches));
	}

	@Override
	public <O> Ref<O> branch(Ref<Boolean> condition, Reader<? extends O> ifTrue, Reader<? extends O> ifFalse) {
		SimpleRef<O> ref = SimpleRef.create();
		addOperation(Operations.branch(condition.map(c -> c ? 0 : 1), ifTrue == null ? null : Operations.read(ref, ifTrue), ifFalse == null ? null : Operations.read(ref, ifFalse)));
		return ref;
	}

	@Override
	public <O> Ref<O> branch(Ref<Integer> value, @SuppressWarnings("unchecked") Reader<? extends O>... branches) {
		SimpleRef<O> ref = SimpleRef.create();
		addOperation(Operations.branch(value, Arrays.stream(branches).map(b -> b == null ? null : Operations.read(ref, b)).toArray(Operation[]::new)));
		return ref;
	}

	@Override
	public void loopUntil(Ref<Boolean> condition, Operation body) {
		addOperation(Operations.loopUntil(condition, body));
	}

	@Override
	public <O> Ref<List<O>> loopUntil(Ref<Boolean> condition, Reader<O> body) {
		SimpleRef<O> current = SimpleRef.create();
		SimpleRef<List<O>> ref = SimpleRef.create();
		addOperation(Operations.run(instance -> ref.set(instance, new ArrayList<>())).then(Operations.loopUntil(condition, Operations.read(current, body).thenRun(instance -> ref.get(instance).add(current.get(instance))))));
		return ref;
	}

	@Override
	public void run(UnsafeRunnable action) {
		addOperation(Operations.run(action));
	}

	@Override
	public void run(UnsafeConsumer<ReaderInstance<?>> action) {
		addOperation(Operations.run(action));
	}

	@Override
	public Expectation expect(Ref<Boolean> condition) {
		ExpectationImpl expectation = new ExpectationImpl();
		addOperation(Operations.stateless((instance, buf) -> {
			if(!condition.get(instance)) expectation.fail(instance);
			return true;
		}));
		return expectation;
	}

	@Override
	public Expectation expectByte(byte b) {
		Ref<Byte> read = readByte();
		return expect(instance -> read.get(instance) == b);
	}

	@Override
	public Expectation expectByte(Ref<Byte> b) {
		Ref<Byte> read = readByte();
		return expect(instance -> read.get(instance) == b.get(instance));
	}

	@Override
	public Expectation expectBytes(byte[] bytes) {
		Ref<byte[]> read = readNBytes(bytes.length);
		return expect(instance -> Arrays.equals(read.get(instance), bytes));
	}

	@Override
	public Expectation expectBytes(Ref<byte[]> bytes) {
		SimpleRef<byte[]> read = SimpleRef.create();
		addOperation(Operations.lazy(instance -> Operations.readNBytes(read, bytes.get(instance).length)));
		return expect(instance -> Arrays.equals(read.get(instance), bytes.get(instance)));
	}

	@Override
	public void setConverter(UnsafeFunction<ReaderInstance<T>, T> converter) {
		this.converter = converter;
	}

	@Override
	public ReaderInstance<T> createInstance() {
		return new Instance();
	}

	protected class Instance implements ReaderInstance<T> {

		private List<Operation> operations;
		private Map<Operation, StackTraceElement[]> stackTraces;
		private int idx;
		private Map<Ref<?>, Object> refValues;
		private boolean finished;
		private T value;
		private Consumer<T> onFinished;

		public Instance() {
			reset();
		}

		@Override
		public boolean read(ByteBuffer buffer) throws IOException {
			if(idx >= operations.size()) throw new IllegalStateException("Read after finished");

			while(idx < operations.size()) {
				try {
					Operation operation = operations.get(idx);
					if(operation.read(this, buffer)) {
						idx++;
					}else if(!buffer.hasRemaining()) break;
				}catch(Exception e) {
					IOException ex = new IOException("Operation at index " + idx + " failed", e);
					ex.setStackTrace(stackTraces.get(operations.get(idx)));
					throw ex;
				}
			}

			if(idx == operations.size()) {
				finished();
				return true;
			}

			return false;
		}

		@Override
		public ReaderInstance<T> onFinished(Consumer<T> consumer) {
			this.onFinished = consumer;
			return this;
		}

		private void finished() throws IOException {
			finished = true;
			value = converter.apply(this);
			if(onFinished != null) onFinished.accept(value);
		}

		@Override
		public T get() throws IOException {
			if(!finished) throw new IllegalStateException("Reader is not finished");
			return value;
		}

		@Override
		public <R> void setRef(SimpleRef<R> ref, R value) throws IllegalStateException {
			refValues.put(ref, value);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <R> R getRef(SimpleRef<R> ref) throws IllegalStateException {
			if(!refValues.containsKey(ref)) throw new IllegalStateException("Tried to access ref before it was set (make sure you only call get() in a callback)");
			return (R) refValues.get(ref);
		}

		@Override
		public void reset() {
			this.stackTraces = new HashMap<>();
			this.operations = ReaderImpl.this.operations.stream().map(op -> {
				Operation copy = op.copy();
				stackTraces.put(copy, ReaderImpl.this.stackTraces.get(op));
				return copy;
			}).collect(Collectors.toList());
			this.idx = 0;
			this.refValues = new HashMap<>();
			this.finished = false;
			this.value = null;
		}

	}

}
