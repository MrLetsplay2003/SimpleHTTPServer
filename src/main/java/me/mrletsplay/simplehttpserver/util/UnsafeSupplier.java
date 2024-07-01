package me.mrletsplay.simplehttpserver.util;

import java.io.IOException;

public interface UnsafeSupplier<T> {

	public T get() throws IOException;

}
