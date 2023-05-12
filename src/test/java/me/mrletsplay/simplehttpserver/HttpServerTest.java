package me.mrletsplay.simplehttpserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import me.mrletsplay.mrcore.http.HttpRequest;
import me.mrletsplay.mrcore.http.HttpResult;
import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.server.HttpServer;
import me.mrletsplay.simplehttpserver.http.server.HttpServerConfiguration;
import me.mrletsplay.simplehttpserver.http.util.MimeType;

public class HttpServerTest {

	private HttpServer server;

	@BeforeEach
	public void initServer() {
		server = new HttpServer(new HttpServerConfiguration.Builder()
			.port(12345)
			.host("127.0.0.1")
			.debugMode(true)
			.create());

		server.start();
	}

	@AfterEach
	public void stopServer() {
		server.shutdown();
	}

	@Test
	public void testRequests() {
		HttpResult res = HttpRequest.createGet("http://127.0.0.1:12345/test").execute();
		assertEquals(res.getStatusCode(), 404);
		assertTrue(res.asRaw().length > 0, "Error response includes text");
	}

	@Test
	public void testDocumentPaths() {
		server.getDocumentProvider().register(HttpRequestMethod.GET, "/test", () -> {
			HttpRequestContext.getCurrentContext().getServerHeader().setContent(MimeType.TEXT, "Hello World!".getBytes(StandardCharsets.UTF_8));
		});

		server.getDocumentProvider().registerPattern(HttpRequestMethod.GET, "/pattern/{name}", () -> {
			HttpRequestContext ctx = HttpRequestContext.getCurrentContext();
			ctx.getServerHeader().setContent(MimeType.TEXT, ctx.getPathParameters().get("name").getBytes(StandardCharsets.UTF_8));
		});

		assertEquals("Hello World!", HttpRequest.createGet("http://127.0.0.1:12345/test").execute().asString());
		assertEquals("one", HttpRequest.createGet("http://127.0.0.1:12345/pattern/one").execute().asString());
		assertEquals("two", HttpRequest.createGet("http://127.0.0.1:12345/pattern/two").execute().asString());
	}

}
