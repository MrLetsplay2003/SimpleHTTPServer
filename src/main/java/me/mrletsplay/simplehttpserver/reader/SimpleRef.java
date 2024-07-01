package me.mrletsplay.simplehttpserver.reader;

public class SimpleRef<T> implements Ref<T> {

	private boolean rewritable;

	private SimpleRef(boolean rewritable) {
		this.rewritable = rewritable;
	}

	public void set(ReaderInstance<?> instance, T value) throws IllegalStateException {
		instance.setRef(this, value, rewritable);
	}

	@Override
	public T get(ReaderInstance<?> instance) throws IllegalStateException {
		return instance.getRef(this);
	}

	public static <T> SimpleRef<T> create() {
		return new SimpleRef<>(false);
	}

	public static <T> SimpleRef<T> createRewritable() {
		return new SimpleRef<>(true);
	}

}
