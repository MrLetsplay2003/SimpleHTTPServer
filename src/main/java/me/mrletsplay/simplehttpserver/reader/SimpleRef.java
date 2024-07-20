package me.mrletsplay.simplehttpserver.reader;

public class SimpleRef<T> implements Ref<T> {

	private SimpleRef() {}

	public void set(ReaderInstance<?> instance, T value) {
		instance.setRef(this, value);
	}

	@Override
	public boolean isSet(ReaderInstance<?> instance) {
		return instance.isRefSet(this);
	}

	@Override
	public T get(ReaderInstance<?> instance) throws IllegalStateException {
		return instance.getRef(this);
	}

	@Override
	public T getOrElse(ReaderInstance<?> instance, T other) {
		if(!instance.isRefSet(this)) return other;
		return get(instance);
	}

	public static <T> SimpleRef<T> create() {
		return new SimpleRef<>();
	}

}
