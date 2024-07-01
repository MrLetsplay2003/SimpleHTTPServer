package me.mrletsplay.simplehttpserver.util;

import java.io.IOException;

public interface UnsafeFunction<I, O> {

	public O apply(I i) throws IOException;

}
