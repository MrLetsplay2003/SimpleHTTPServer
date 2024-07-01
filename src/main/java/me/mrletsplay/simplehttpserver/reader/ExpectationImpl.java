package me.mrletsplay.simplehttpserver.reader;

import java.io.IOException;
import java.util.function.Supplier;

import me.mrletsplay.simplehttpserver.util.UnsafeRunnable;

public class ExpectationImpl implements Expectation {

	private UnsafeRunnable run;

	@Override
	public void orElseRun(UnsafeRunnable run) {
		this.run = run;
	}

	@Override
	public void orElseThrow(Supplier<? extends IOException> exception) {
		this.run = () -> { throw exception.get(); };
	}

	public void fail() throws IOException {
		if(run != null) run.run();
	}

}
