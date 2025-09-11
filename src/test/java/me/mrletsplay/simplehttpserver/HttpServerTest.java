package me.mrletsplay.simplehttpserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import me.mrletsplay.mrcore.http.HttpRequest;
import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;
import me.mrletsplay.simplehttpserver.http.document.StringDocument;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.server.HttpServer;
import me.mrletsplay.simplehttpserver.http.server.HttpServerConfiguration;
import me.mrletsplay.simplehttpserver.http.util.MimeType;

public class HttpServerTest {

	private static HttpServer server;

	@BeforeAll
	public static void startServer() {
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "TRACE");

		server = new HttpServer(new HttpServerConfiguration.Builder()
			.port(12345)
			.host("127.0.0.1")
			.poolSize(2)
			.ioWorkers(1)
			.debugMode(true)
			.create());

		server.getDocumentProvider().register(HttpRequestMethod.GET, "/test", new StringDocument("Hello World!"));
		server.getDocumentProvider().register(HttpRequestMethod.GET, "/foo", new StringDocument("foobar123"));

		server.getDocumentProvider().registerPattern(HttpRequestMethod.GET, "/pattern/{name}", () -> {
			HttpRequestContext ctx = HttpRequestContext.getCurrentContext();
			ctx.getServerHeader().setContent(MimeType.TEXT, ctx.getPathParameters().get("name").getBytes(StandardCharsets.UTF_8));
		});

		server.start();
	}

	@AfterAll
	public static void stopServer() {
		server.shutdown();
	}

	@Test
	public void testDocumentPaths() {
		assertEquals("Hello World!", HttpRequest.createGet("http://127.0.0.1:12345/test").execute().asString());
		assertEquals("one", HttpRequest.createGet("http://127.0.0.1:12345/pattern/one").execute().asString());
		assertEquals("two", HttpRequest.createGet("http://127.0.0.1:12345/pattern/two").execute().asString());
	}

	@Test
	public void testCloseAfterWrite() throws IOException {
		try(Socket socket = new Socket("127.0.0.1", 12345)) {
			socket.getOutputStream().write((
				"GET /test HTTP/1.1\r\n"
				+ "Host: 127.0.0.1\r\n"
				+ "Connection: close\r\n"
				+ "\r\n"
			).getBytes(StandardCharsets.UTF_8));

			socket.shutdownOutput(); // Close write side

			assertFalse(socket.isClosed(), "Socket must remain open until the response is sent");

			byte[] response = socket.getInputStream().readAllBytes();
			assertTrue(new String(response, StandardCharsets.UTF_8).contains("Hello World!"), "Response must contain a body");
		}
	}

	@Test
	public void testPipelinedRequest() throws IOException {
		try(Socket socket = new Socket("127.0.0.1", 12345)) {
			socket.getOutputStream().write((
				"GET /test HTTP/1.1\r\n"
				+ "Host: 127.0.0.1\r\n"
				+ "Connection: keep-alive\r\n"
				+ "\r\n"
				+ "GET /foo HTTP/1.1\r\n"
				+ "Host: 127.0.0.1\r\n"
				+ "Connection: close\r\n"
				+ "\r\n"
			).getBytes(StandardCharsets.UTF_8));

			socket.shutdownOutput(); // Close write side

			assertFalse(socket.isClosed(), "Socket must remain open until the response is sent");

			byte[] response = socket.getInputStream().readAllBytes();
			assertTrue(new String(response, StandardCharsets.UTF_8).contains("Hello World!"), "First response must be present");
			assertTrue(new String(response, StandardCharsets.UTF_8).contains("foobar123"), "Second response must be present");
		}
	}

	@Test
	public void testPipelinedClose() throws IOException {
		try(Socket socket = new Socket("127.0.0.1", 12345)) {
			socket.getOutputStream().write((
				"GET /test HTTP/1.1\r\n"
				+ "Host: 127.0.0.1\r\n"
				+ "Connection: close\r\n" // Connection already closed here, second response should be omitted
				+ "\r\n"
				+ "GET /foo HTTP/1.1\r\n"
				+ "Host: 127.0.0.1\r\n"
				+ "Connection: close\r\n"
				+ "\r\n"
			).getBytes(StandardCharsets.UTF_8));

			socket.shutdownOutput(); // Close write side

			assertFalse(socket.isClosed(), "Socket must remain open until the response is sent");

			byte[] response = socket.getInputStream().readAllBytes();
			assertTrue(new String(response, StandardCharsets.UTF_8).contains("Hello World!"), "First response must be present");
			assertFalse(new String(response, StandardCharsets.UTF_8).contains("foobar123"), "Second response should be omitted");
		}
	}

}
