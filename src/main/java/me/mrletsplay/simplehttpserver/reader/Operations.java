package me.mrletsplay.simplehttpserver.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import me.mrletsplay.simplehttpserver.util.UnsafeConsumer;
import me.mrletsplay.simplehttpserver.util.UnsafeRunnable;

public class Operations {

	public static Operation stateless(OperationCallback callback) {
		return new Operation() {

			@Override
			public boolean read(ReaderInstance<?> instance, ByteBuffer buf) throws IOException {
				return callback.read(instance, buf);
			}

			@Override
			public Operation copy() {
				return this;
			}

			@Override
			public void reset() {

			}

		};
	}

	public static Operation run(UnsafeRunnable run) {
		return stateless((instance, buf) -> {
			run.run();
			return true;
		});
	}

	public static Operation run(UnsafeConsumer<ReaderInstance<?>> run) {
		return stateless((instance, buf) -> {
			run.accept(instance);
			return true;
		});
	}

	public static Operation allOf(Operation... operations) {
		return new CombinedOperation(operations);
	}

	public static Operation readByte(SimpleRef<Byte> ref) {
		return stateless((instance, buf) -> {
			if(!buf.hasRemaining()) return false;
			ref.set(instance, buf.get());
			return true;
		});
	}

	public static Operation readInt(SimpleRef<Integer> ref) {
		return new ReadIntOperation(ref);
	}

	public static Operation readNBytes(SimpleRef<byte[]> ref, int n) {
		return new ReadBytesOperation(ref, n);
	}

	public static Operation readUntilBytes(SimpleRef<byte[]> ref, byte[] delimiter, int limit) {
		return new ReadBytesUntilOperation(ref, delimiter, limit);
	}

	public static Operation lazy(Ref<Operation> lazy) {
		return new LazyOperation(lazy);
	}

	public static Operation branch(Ref<Integer> value, Operation... branches) {
		return new BranchOperation(value, branches);
	}

	public static Operation loopUntil(Ref<Boolean> condition, Operation body) {
		return new LoopUntilOperation(condition, body);
	}

	public static <T> Operation read(SimpleRef<T> ref, Reader<? extends T> reader) {
		return new ReaderOperation<>(ref, reader);
	}

	private static class CombinedOperation implements Operation {

		private Operation[] operations;
		private int i;

		public CombinedOperation(Operation... operations) {
			this.operations = operations;
			reset();
		}

		@Override
		public boolean read(ReaderInstance<?> instance, ByteBuffer buf) throws IOException {
			while(i < operations.length) {
				if(operations[i].read(instance, buf)) {
					i++;
				}else if(!buf.hasRemaining()) break;
			}

			return i == operations.length;
		}

		@Override
		public Operation copy() {
			return new CombinedOperation(Arrays.stream(operations).map(Operation::copy).toArray(Operation[]::new));
		}

		@Override
		public void reset() {
			for(Operation op : operations) op.reset();
			i = 0;
		}

	}

	private static class ReadIntOperation implements Operation {

		private SimpleRef<Integer> ref;
		private int value;
		private int i;

		public ReadIntOperation(SimpleRef<Integer> ref) {
			this.ref = ref;
			reset();
		}

		@Override
		public boolean read(ReaderInstance<?> instance, ByteBuffer buf) throws IOException {
			while(buf.hasRemaining() && i > 0) {
				i--;
				value |= (buf.get() & 0xFF) << i;
			}

			if(i == 0) {
				ref.set(instance, value);
				return true;
			}

			return false;
		}

		@Override
		public Operation copy() {
			return new ReadIntOperation(ref);
		}

		@Override
		public void reset() {
			this.value = 0;
			this.i = 4;
		}

	}

	private static class ReadBytesOperation implements Operation {

		private SimpleRef<byte[]> ref;
		private byte[] bytes;
		private int i;

		public ReadBytesOperation(SimpleRef<byte[]> ref, int n) {
			this.ref = ref;
			this.bytes = new byte[n];
			reset();
		}

		@Override
		public boolean read(ReaderInstance<?> instance, ByteBuffer buf) throws IOException {
			while(buf.hasRemaining() && i < bytes.length) {
				bytes[i++] = buf.get();
			}

			if(i == bytes.length) {
				ref.set(instance, bytes);
				return true;
			}

			return false;
		}

		@Override
		public Operation copy() {
			return new ReadBytesOperation(ref, bytes.length);
		}

		@Override
		public void reset() {
			i = 0;
		}

	}

	private static class ReadBytesUntilOperation implements Operation {

		private SimpleRef<byte[]> ref;
		private byte[] delimiter;
		private byte[] bytes;
		private int i;

		public ReadBytesUntilOperation(SimpleRef<byte[]> ref, byte[] delimiter, int limit) {
			this.ref = ref;
			this.delimiter = delimiter;
			this.bytes = new byte[limit];
			reset();
		}

		@Override
		public boolean read(ReaderInstance<?> instance, ByteBuffer buf) throws IOException {
			while(buf.hasRemaining() && i < bytes.length) {
				bytes[i++] = buf.get();

				if(i >= delimiter.length && Arrays.equals(bytes, i - delimiter.length, i, delimiter, 0, delimiter.length)) {
					ref.set(instance, Arrays.copyOfRange(bytes, 0, i));
					return true;
				}
			}

			if(i == bytes.length) throw new IOException("Buffer limit reached");

			return false;
		}

		@Override
		public Operation copy() {
			return new ReadBytesUntilOperation(ref, delimiter, bytes.length);
		}

		@Override
		public void reset() {
			i = 0;
		}

	}

	private static class LazyOperation implements Operation {

		private Ref<Operation> operationSupplier;
		private Operation operation;

		public LazyOperation(Ref<Operation> operationSupplier) {
			this.operationSupplier = operationSupplier;
			reset();
		}

		@Override
		public boolean read(ReaderInstance<?> instance, ByteBuffer buf) throws IOException {
			if(operation == null) operation = operationSupplier.get(instance);
			return operation.read(instance, buf);
		}

		@Override
		public Operation copy() {
			return new LazyOperation(operationSupplier);
		}

		@Override
		public void reset() {
			this.operation = null;
		}

	}

	private static class BranchOperation implements Operation {

		private Ref<Integer> value;
		private Operation[] branches;
		private int i;

		public BranchOperation(Ref<Integer> value, Operation... branches) {
			this.value = value;
			this.branches = branches;
			reset();
		}

		@Override
		public boolean read(ReaderInstance<?> instance, ByteBuffer buf) throws IOException {
			if(i == -1) i = value.get(instance);
			if(branches[i] == null) return true;
			return branches[i].read(instance, buf);
		}

		@Override
		public Operation copy() {
			return new BranchOperation(value, Arrays.stream(branches).map(op -> op == null ? null : op.copy()).toArray(Operation[]::new));
		}

		@Override
		public void reset() {
			for(Operation op : branches) {
				if(op != null) op.reset();
			}
			this.i = -1;
		}

	}

	private static class LoopUntilOperation implements Operation {

		private Ref<Boolean> condition;
		private Operation body;

		public LoopUntilOperation(Ref<Boolean> condition, Operation body) {
			this.condition = condition;
			this.body = body;
			reset();
		}

		@Override
		public boolean read(ReaderInstance<?> instance, ByteBuffer buf) throws IOException {
			if(body.read(instance, buf)) {
				body.reset();
				if(condition.get(instance)) return true;
			}

			return false;
		}

		@Override
		public Operation copy() {
			return new LoopUntilOperation(condition, body.copy());
		}

		@Override
		public void reset() {
			body.reset();
		}

	}

	private static class ReaderOperation<T> implements Operation {

		private SimpleRef<T> ref;
		private Reader<? extends T> reader;
		private ReaderInstance<? extends T> readerInstance;

		public ReaderOperation(SimpleRef<T> ref, Reader<? extends T> reader) {
			this.ref = ref;
			this.reader = reader;
			this.readerInstance = reader.createInstance();
			reset();
		}

		@Override
		public boolean read(ReaderInstance<?> instance, ByteBuffer buf) throws IOException {
			if(readerInstance.read(buf)) {
				ref.set(instance, readerInstance.get());
				return true;
			}

			return false;
		}

		@Override
		public Operation copy() {
			return new ReaderOperation<>(ref, reader);
		}

		@Override
		public void reset() {
			readerInstance.reset();
		}

	}

}
