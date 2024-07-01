package me.mrletsplay.simplehttpserver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import me.mrletsplay.simplehttpserver.http.header.HttpClientHeader;
import me.mrletsplay.simplehttpserver.http.reader.HttpReaders;
import me.mrletsplay.simplehttpserver.reader.Reader;
import me.mrletsplay.simplehttpserver.reader.ReaderInstance;

public class HttpReadersTest {

	@Test
	public void testClientHeaderReader() {
		String httpHeader = "GET / HTTP/1.1\r\n"
			+ "Host: example.com\r\n"
			+ "\r\n";

		Reader<HttpClientHeader> reader = HttpReaders.clientHeaderReader();
		ReaderInstance<HttpClientHeader> readerInstance = reader.createInstance();
		readerInstance.onFinished(header -> System.out.println(header.getFields().getFirst("Host")));
		try {
			readerInstance.read(ByteBuffer.wrap(httpHeader.getBytes(StandardCharsets.UTF_8)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
//		assertDoesNotThrow(() -> );
	}

}
