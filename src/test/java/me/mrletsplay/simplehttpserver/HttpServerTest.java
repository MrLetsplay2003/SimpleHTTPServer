package me.mrletsplay.simplehttpserver;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.Test;

import me.mrletsplay.mrcore.http.HttpRequest;
import me.mrletsplay.simplehttpserver.http.HttpServer;
import me.mrletsplay.simplehttpserver.http.HttpServerConfiguration;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;

public class HttpServerTest {

	@Test
	public void testDocumentPaths() {
		HttpServer server = new HttpServer(new HttpServerConfiguration.Builder()
			.port(12345)
			.host("127.0.0.1")
			.debugMode(true)
			.create());

		server.getDocumentProvider().registerDocument("/test", () -> {
			HttpRequestContext.getCurrentContext().getServerHeader().setContent("text/plain", "Hello World!".getBytes(StandardCharsets.UTF_8));
		});

		server.getDocumentProvider().registerDocumentPattern("/pattern/{name}", () -> {
			HttpRequestContext ctx = HttpRequestContext.getCurrentContext();
			ctx.getServerHeader().setContent("text/plain", ctx.getPathParameters().get("name").getBytes(StandardCharsets.UTF_8));
		});

		server.start();

		assertEquals("Hello World!", HttpRequest.createGet("http://localhost:12345/test").execute().asString());
		assertEquals("one", HttpRequest.createGet("http://localhost:12345/pattern/one").execute().asString());
		assertEquals("two", HttpRequest.createGet("http://localhost:12345/pattern/two").execute().asString());

		server.shutdown();
	}

}
