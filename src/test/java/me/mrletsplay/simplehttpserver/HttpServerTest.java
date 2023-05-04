package me.mrletsplay.simplehttpserver;

import org.junit.Test;

public class HttpServerTest {

	@Test
	public void testDocumentPaths() {
//		HttpServer server = new HttpServer(new HttpServerConfiguration.Builder()
//			.port(12345)
//			.host("127.0.0.1")
//			.debugMode(true)
//			.create());
//
//		server.getDocumentProvider().register(HttpRequestMethod.GET, "/test", () -> {
//			HttpRequestContext.getCurrentContext().getServerHeader().setContent(MimeType.TEXT, "Hello World!".getBytes(StandardCharsets.UTF_8));
//		});
//
//		server.getDocumentProvider().registerPattern(HttpRequestMethod.GET, "/pattern/{name}", () -> {
//			HttpRequestContext ctx = HttpRequestContext.getCurrentContext();
//			ctx.getServerHeader().setContent(MimeType.TEXT, ctx.getPathParameters().get("name").getBytes(StandardCharsets.UTF_8));
//		});
//
//		server.start();
//
//		assertEquals("Hello World!", HttpRequest.createGet("http://localhost:12345/test").execute().asString());
//		assertEquals("one", HttpRequest.createGet("http://localhost:12345/pattern/one").execute().asString());
//		assertEquals("two", HttpRequest.createGet("http://localhost:12345/pattern/two").execute().asString());
//
//		server.shutdown();
	}

}
