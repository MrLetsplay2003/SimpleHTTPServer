package me.mrletsplay.simplehttpserver._test;

import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.simplehttpserver.dom.html.HtmlDocument;
import me.mrletsplay.simplehttpserver.http.HttpServer;

public class SimpleHttpServerMain {

	public static void main(String[] args) {
		HttpServer server = new HttpServer(HttpServer.newConfigurationBuilder()
			.host("localhost")
			.port(8765)
			.debugMode(true)
			.create());

		HtmlDocument d = new HtmlDocument();
		d.setTitle("Hello World!");
		d.getBodyNode().setText("Hello World!");
		server.getDocumentProvider().registerDocument("/", d);
		server.getDocumentProvider().registerDocument("/error", () -> {
			throw new FriendlyException("test");
		});

		server.start();
	}

}
