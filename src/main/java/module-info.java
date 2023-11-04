module simplehttpserver {
	exports me.mrletsplay.simplehttpserver.php;
	exports me.mrletsplay.simplehttpserver.server;
	exports me.mrletsplay.simplehttpserver.server.connection;
	exports me.mrletsplay.simplehttpserver.server.impl;
	exports me.mrletsplay.simplehttpserver.dom.html;
	exports me.mrletsplay.simplehttpserver.dom.html.element;
	exports me.mrletsplay.simplehttpserver.dom.css;
	exports me.mrletsplay.simplehttpserver.dom.js;
	exports me.mrletsplay.simplehttpserver.http;
	exports me.mrletsplay.simplehttpserver.http.validation;
	exports me.mrletsplay.simplehttpserver.http.validation.result;
	exports me.mrletsplay.simplehttpserver.http.response;
	exports me.mrletsplay.simplehttpserver.http.request;
	exports me.mrletsplay.simplehttpserver.http.request.urlencoded;
	exports me.mrletsplay.simplehttpserver.http.request.multipart;
	exports me.mrletsplay.simplehttpserver.http.request.form;
	exports me.mrletsplay.simplehttpserver.http.header;
	exports me.mrletsplay.simplehttpserver.http.compression;
	exports me.mrletsplay.simplehttpserver.http.server;
	exports me.mrletsplay.simplehttpserver.http.server.connection;
	exports me.mrletsplay.simplehttpserver.http.ssl;
	exports me.mrletsplay.simplehttpserver.http.util;
	exports me.mrletsplay.simplehttpserver.http.exception;
	exports me.mrletsplay.simplehttpserver.http.endpoint;
	exports me.mrletsplay.simplehttpserver.http.endpoint.exception;
	exports me.mrletsplay.simplehttpserver.http.endpoint.rest;
	exports me.mrletsplay.simplehttpserver.http.websocket;
	exports me.mrletsplay.simplehttpserver.http.websocket.frame;
	exports me.mrletsplay.simplehttpserver.http.document;

	requires transitive me.mrletsplay.mrcore;
	requires transitive org.slf4j;

	requires org.apache.commons.text;
}