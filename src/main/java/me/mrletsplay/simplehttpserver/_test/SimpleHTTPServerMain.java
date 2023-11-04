package me.mrletsplay.simplehttpserver._test;

import java.io.File;
import java.util.Arrays;

import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;
import me.mrletsplay.simplehttpserver.http.document.DocumentProvider;
import me.mrletsplay.simplehttpserver.http.document.StringDocument;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.server.HttpsServer;
import me.mrletsplay.simplehttpserver.http.websocket.WebSocketConnection;
import me.mrletsplay.simplehttpserver.http.websocket.WebSocketEndpoint;
import me.mrletsplay.simplehttpserver.http.websocket.frame.CloseFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketFrame;

public class SimpleHTTPServerMain {

	public static void main(String[] args) {
//		HttpServer srv = new HttpServer(HttpServer.newConfigurationBuilder()
//			.host("localhost")
//			.port(8080)
//			.poolSize(20)
////			.certificate(new File("TEST/certificate.pem"), new File("TEST/privatekey.pem"))
//			.create());

		HttpsServer srv = new HttpsServer(HttpsServer.newConfigurationBuilder()
			.host("localhost")
			.port(8080)
			.poolSize(20)
			.certificate(new File("TEST/certificate.pem"), new File("TEST/privatekey.pem"))
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

		WebSocketEndpoint wsEp = new WebSocketEndpoint() {

			@Override
			public void onOpen(WebSocketConnection connection) {
				System.out.println("opened");
			}

			@Override
			public void onClose(WebSocketConnection connection, CloseFrame closeFrame) {
				System.out.println("closed");
			}

			@Override
			public void onFrameReceived(WebSocketConnection connection, WebSocketFrame frame) {
				System.out.println("frame received: " + frame.getOpCode());
			}

			@Override
			public void onTextMessage(WebSocketConnection connection, String message) {
				System.out.println("text: " + message);
				connection.sendText(message + "!");

				if(message.equals("bye")) {
					connection.close(CloseFrame.GOING_AWAY, "Goodbye");
				}
			}

			@Override
			public void onBinaryMessage(WebSocketConnection connection, byte[] message) {
				System.out.println("binary: " + Arrays.toString(message));
				connection.sendBinary(message);
			}

		};
		provider.register(HttpRequestMethod.GET, "/ws", wsEp);

		srv.start();
	}

}
