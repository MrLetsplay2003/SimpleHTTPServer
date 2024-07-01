package me.mrletsplay.simplehttpserver.util;

import java.io.IOException;

public interface UnsafeConsumer<T> {

	public void accept(T value) throws IOException;

}
