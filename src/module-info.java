module simplehttpserver {
	exports me.mrletsplay.simplehttpserver.http.document;
	exports me.mrletsplay.simplehttpserver.http.compression;
	exports me.mrletsplay.simplehttpserver.http.ssl;
	exports me.mrletsplay.simplehttpserver.http.header;
	exports me.mrletsplay.simplehttpserver.server.impl;
	exports me.mrletsplay.simplehttpserver.dom.html.element;
	exports me.mrletsplay.simplehttpserver._test;
	exports me.mrletsplay.simplehttpserver.http.request.urlencoded;
	exports me.mrletsplay.simplehttpserver.http.websocket;
	exports me.mrletsplay.simplehttpserver.php;
	exports me.mrletsplay.simplehttpserver.http.request;
	exports me.mrletsplay.simplehttpserver.http.websocket.frame;
	exports me.mrletsplay.simplehttpserver.http.request.multipart;
	exports me.mrletsplay.simplehttpserver.server.connection;
	exports me.mrletsplay.simplehttpserver.dom.js;
	exports me.mrletsplay.simplehttpserver.http;
	exports me.mrletsplay.simplehttpserver.server;
	exports me.mrletsplay.simplehttpserver.dom.html;
	exports me.mrletsplay.simplehttpserver.http.request.form;
	exports me.mrletsplay.simplehttpserver.dom.css;

	requires transitive mrcore;
	requires transitive org.slf4j;

	requires org.apache.commons.text;
}