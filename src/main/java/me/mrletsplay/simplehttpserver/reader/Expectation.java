package me.mrletsplay.simplehttpserver.reader;

import java.io.IOException;
import java.util.function.Supplier;

import me.mrletsplay.simplehttpserver.util.UnsafeRunnable;

public interface Expectation {

	public void orElseRun(UnsafeRunnable run);

	public void orElseThrow(Supplier<? extends IOException> exception);

}
