package me.mrletsplay.simplehttpserver.reader;

public class SimpleRef<T> implements Ref<T> {

	private SimpleRef() {}

	public void set(ReaderInstance<?> instance, T value) throws IllegalStateException {
		instance.setRef(this, value);
	}

	@Override
	public T get(ReaderInstance<?> instance) throws IllegalStateException {
		return instance.getRef(this);
	}

	public static <T> SimpleRef<T> create() {
		return new SimpleRef<>();
	}

}
