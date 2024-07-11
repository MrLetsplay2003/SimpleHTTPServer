package me.mrletsplay.simplehttpserver.reader;

import java.io.IOException;
import java.util.function.Supplier;

import me.mrletsplay.simplehttpserver.util.UnsafeConsumer;
import me.mrletsplay.simplehttpserver.util.UnsafeRunnable;

public class ExpectationImpl implements Expectation {

	private UnsafeConsumer<ReaderInstance<?>> run;

	@Override
	public void orElseRun(UnsafeConsumer<ReaderInstance<?>> run) {
		this.run = run;
	}

	@Override
	public void orElseRun(UnsafeRunnable run) {
		this.run = instance -> run.run();
	}

	@Override
	public void orElseThrow(Supplier<? extends IOException> exception) {
		this.run = instance -> { throw exception.get(); };
	}

	public void fail(ReaderInstance<?> instance) throws IOException {
		if(run == null) throw new IOException("Expectation failed");
		run.accept(instance);
	}

}
