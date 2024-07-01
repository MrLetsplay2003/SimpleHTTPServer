package me.mrletsplay.simplehttpserver.reader;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * Readonly representation of a reference.
 * @param <T> The referenced type
 */
public interface Ref<T> {

	public T get(ReaderInstance<?> instance) throws IllegalStateException;

	public default <O> Ref<O> map(Function<T, O> map) {
		return instance -> map.apply(get(instance));
	}

	public default boolean isSet(ReaderInstance<?> instance) {
		try {
			get(instance);
			return true;
		}catch(IllegalStateException e) {
			return false;
		}
	}

	public default T getOrElse(ReaderInstance<?> instance, T other) {
		try {
			return get(instance);
		}catch(IllegalStateException e) {
			return other;
		}
	}

	public static Ref<String> asString(Ref<byte[]> bytes, Charset charset) {
		return bytes.map(b -> new String(b, charset));
	}

	public static Ref<String> asString(Ref<byte[]> bytes) {
		return asString(bytes, StandardCharsets.UTF_8);
	}

	public static Ref<byte[]> asBytes(Ref<String> string, Charset charset) {
		return string.map(s -> s.getBytes(charset));
	}

	public static Ref<byte[]> asBytes(Ref<String> string) {
		return asBytes(string, StandardCharsets.UTF_8);
	}

}
