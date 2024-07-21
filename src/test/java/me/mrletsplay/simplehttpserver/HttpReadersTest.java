package me.mrletsplay.simplehttpserver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;
import me.mrletsplay.simplehttpserver.http.header.DefaultClientContentTypes;
import me.mrletsplay.simplehttpserver.http.header.HttpClientHeader;
import me.mrletsplay.simplehttpserver.http.reader.HttpReaders;
import me.mrletsplay.simplehttpserver.reader.Reader;
import me.mrletsplay.simplehttpserver.reader.ReaderInstance;

public class HttpReadersTest {

	private HttpClientHeader parse(String data) throws IOException {
		AtomicReference<HttpClientHeader> parsedHeader = new AtomicReference<>();

		Reader<HttpClientHeader> reader = HttpReaders.CLIENT_HEADER_READER;
		ReaderInstance<HttpClientHeader> readerInstance = reader.createInstance();
		readerInstance.onFinished(parsedHeader::set);
		readerInstance.read(ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8)));

		return parsedHeader.get();
	}

	@Test
	public void testClientHeaderReaderSimple() {
		String httpHeader = "GET / HTTP/1.1\r\n"
			+ "Host: example.com\r\n"
			+ "X-Amogus: sus\r\n"
			+ "a: b\r\n"
			+ "a: c\r\n"
			+ "\r\n";

		HttpClientHeader header = assertDoesNotThrow(() -> parse(httpHeader));
		assertNotNull(header);

		assertEquals(HttpRequestMethod.GET, header.getMethod());
		assertEquals("/", header.getPath().toString());
		assertEquals("HTTP/1.1", header.getProtocolVersion());

		assertEquals("example.com", header.getFields().getFirst("Host"));
		assertEquals("sus", header.getFields().getFirst("X-Amogus"));
		assertEquals(List.of("b", "c"), header.getFields().getAll("a"));

		assertNull(header.getPostData().getRaw());
	}

	@Test
	public void testClientHeaderReaderContent() {
		String httpHeader = "GET / HTTP/1.1\r\n"
			+ "Host: example.com\r\n"
			+ "Content-Length: 12\r\n"
			+ "\r\n"
			+ "Hello World!";

		HttpClientHeader header = assertDoesNotThrow(() -> parse(httpHeader));
		assertNotNull(header);

		assertEquals(HttpRequestMethod.GET, header.getMethod());
		assertEquals("/", header.getPath().toString());
		assertEquals("HTTP/1.1", header.getProtocolVersion());

		assertEquals("example.com", header.getFields().getFirst("Host"));

		assertEquals("Hello World!", header.getPostData().getParsedAs(DefaultClientContentTypes.TEXT));
	}

	@Test
	public void testClientHeaderReaderChunked() {
		String httpHeader = "GET / HTTP/1.1\r\n"
			+ "Host: example.com\r\n"
			+ "Transfer-Encoding: chunked\r\n"
			+ "\r\n"
			+ "1\r\n"
			+ "H\r\n"
			+ "b\r\n"
			+ "ello World!\r\n"
			+ "0\r\n"
			+ "\r\n";

		HttpClientHeader header = assertDoesNotThrow(() -> parse(httpHeader));
		assertNotNull(header);

		assertEquals(HttpRequestMethod.GET, header.getMethod());
		assertEquals("/", header.getPath().toString());
		assertEquals("HTTP/1.1", header.getProtocolVersion());

		assertEquals("example.com", header.getFields().getFirst("Host"));

		assertEquals("Hello World!", header.getPostData().getParsedAs(DefaultClientContentTypes.TEXT));
	}

	@Test
	public void testClientHeaderReaderInvalidContent() {
		String httpHeader = "GET / HTTP/1.1\r\n"
			+ "Host: example.com\r\n"
			+ "Content-Length: 12\r\n"
			+ "Transfer-Encoding: chunked\r\n"
			+ "\r\n"
			+ "Hello World!";

		assertThrows(IOException.class, () -> parse(httpHeader), "Headers contain both a content length and a transfer encoding field");
	}

}
