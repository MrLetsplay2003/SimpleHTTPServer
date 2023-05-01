package me.mrletsplay.simplehttpserver._test;

import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;
import me.mrletsplay.simplehttpserver.http.document.DocumentProvider;
import me.mrletsplay.simplehttpserver.http.document.StringDocument;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.server.HttpServer;

public class SimpleHTTPServerMain {

	// TODO: OPTIONS request

	public static void main(String[] args) {
		HttpServer srv = new HttpServer(HttpServer.newConfigurationBuilder()
			.host("localhost")
			.port(8080)
			.poolSize(20)
			.create());

		DocumentProvider provider = srv.getDocumentProvider();
		provider.register(HttpRequestMethod.GET, "/hello", new StringDocument("Hello World"));
		provider.registerPattern(HttpRequestMethod.GET, "/{str}world", () -> new StringDocument("Hello World " + HttpRequestContext.getCurrentContext().getPathParameters().get("str")).createContent());

		System.out.println(provider.getOptions("/hello"));
		System.out.println(provider.getOptions("/helloworld"));
		System.out.println(provider.getOptions("/world"));

		ExampleController cont = new ExampleController();
		System.out.println(cont.getEndpoints());
		cont.register(provider);

		srv.start();
	}

}
