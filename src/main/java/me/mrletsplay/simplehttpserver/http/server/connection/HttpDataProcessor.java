package me.mrletsplay.simplehttpserver.http.server.connection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;
import me.mrletsplay.simplehttpserver.http.HttpStatusCodes;
import me.mrletsplay.simplehttpserver.http.compression.HttpCompressionMethod;
import me.mrletsplay.simplehttpserver.http.cors.CorsConfiguration;
import me.mrletsplay.simplehttpserver.http.document.HttpDocument;
import me.mrletsplay.simplehttpserver.http.exception.HttpResponseException;
import me.mrletsplay.simplehttpserver.http.header.HttpClientHeader;
import me.mrletsplay.simplehttpserver.http.header.HttpHeaderFields;
import me.mrletsplay.simplehttpserver.http.header.HttpServerHeader;
import me.mrletsplay.simplehttpserver.http.reader.HttpReaders;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.request.RequestProcessor;
import me.mrletsplay.simplehttpserver.http.util.MimeType;
import me.mrletsplay.simplehttpserver.http.websocket.WebSocketConnection;
import me.mrletsplay.simplehttpserver.http.websocket.frame.CloseFrame;
import me.mrletsplay.simplehttpserver.reader.ReaderInstance;

public class HttpDataProcessor {

	private HttpConnection connection;

	private WebSocketConnection websocketConnection;

	private LinkedBlockingQueue<RequestAndResponse> responseQueue;

	private ReaderInstance<HttpClientHeader> readerInstance;

	private ReadableByteChannel currentResponse;

	public HttpDataProcessor(HttpConnection connection) {
		this.connection = connection;
		this.responseQueue = new LinkedBlockingQueue<>();
		this.readerInstance = HttpReaders.CLIENT_HEADER_READER.createInstance();
		readerInstance.onFinished(request -> {
			System.out.println("GOT: " + request.getMethod() + " " + request.getPath() + " " + request.getProtocolVersion());
			readerInstance.reset();
			RequestAndResponse requestAndResponse = new RequestAndResponse(request);
			connection.getServer().getExecutor().submit(() -> processRequest(requestAndResponse));
			responseQueue.offer(requestAndResponse);
		});
	}

	public void setWebsocketConnection(WebSocketConnection websocketConnection) {
		this.websocketConnection = websocketConnection;
	}

	public WebSocketConnection getWebsocketConnection() {
		return websocketConnection;
	}

	public void readData(ByteBuffer buffer) throws IOException {
		readerInstance.read(buffer);
	}

	public void writeData(ByteBuffer buffer) throws IOException {
		if(currentResponse == null && responseQueue.isEmpty()) {
			return;
		}

		if(currentResponse == null) {
			if(responseQueue.peek().response == null) {
				// Next response is not yet ready
				return;
			}

			RequestAndResponse requestAndResponse = responseQueue.poll();
			HttpServerHeader response = requestAndResponse.response;
			ByteArrayOutputStream bOut = new ByteArrayOutputStream(); // TODO: improve
			bOut.write(response.getHeaderBytes());
			bOut.write(response.getContent().readAllBytes());
			currentResponse = Channels.newChannel(new ByteArrayInputStream(bOut.toByteArray()));
		}

		if(currentResponse.read(buffer) == -1) {
			currentResponse = null;
		}
	}

	private boolean process(HttpRequestContext context, RequestProcessor process) {
		try {
			return process.process(context);
		}catch(HttpResponseException e) {
			context.setServerHeader(createResponseFromException(e));
			return false;
		}
	}

	private void processRequest(RequestAndResponse requestAndResponse) {
		System.out.println("PROCESS");
		HttpClientHeader request = requestAndResponse.request;

		HttpServerHeader sh = new HttpServerHeader(connection.getServer().getProtocolVersion(), HttpStatusCodes.OK_200, new HttpHeaderFields());
		HttpRequestContext ctx = new HttpRequestContext(connection, request, sh);
		HttpRequestContext.setCurrentContext(ctx);

		HttpDocument d = connection.getServer().getDocumentProvider().get(request.getMethod(), request.getPath().getDocumentPath());
		if(d == null) d = connection.getServer().getDocumentProvider().getNotFoundDocument();

		try {
			boolean cont = true;

			RequestProcessor preProcessor = connection.getServer().getRequestPreProcessor();
			if(preProcessor != null) cont = process(ctx, preProcessor);

			if(cont) cont = process(ctx, this::processCorsPreflight);

			if(cont) cont = process(ctx, this::processCors);

			final HttpDocument document = d;
			if(cont) process(ctx, __ -> { document.createContent(); return true; });

			RequestProcessor postProcessor = connection.getServer().getRequestPostProcessor();
			if(postProcessor != null) process(ctx, postProcessor);

			if(sh.isAllowByteRanges()) process(ctx, this::applyRanges);
			if(sh.isCompressionEnabled()) process(ctx, this::applyCompression);

			sh = ctx.getServerHeader();
		}catch(Exception e) {
			connection.getServer().getLogger().error("Error while processing request", e);

			// Reset all of the context-related fields to ensure a clean environment
			sh = new HttpServerHeader(connection.getServer().getProtocolVersion(), HttpStatusCodes.OK_200, new HttpHeaderFields());
			ctx = new HttpRequestContext(connection, request, sh);
			HttpRequestContext.setCurrentContext(ctx);
			ctx.setException(e);

			connection.getServer().getDocumentProvider().getErrorDocument().createContent();
		}

		requestAndResponse.response = sh;
		System.out.println("REQUEST PROCESSED");
		connection.startWriting();
	}

	private HttpServerHeader createResponseFromException(HttpResponseException exception) {
		HttpServerHeader sh = new HttpServerHeader(connection.getServer().getProtocolVersion(), exception.getStatusCode(), new HttpHeaderFields());
		String statusMessage = exception.getStatusMessage();
		if(statusMessage == null) statusMessage = "Error " + exception.getStatusCode().getStatusCode();
		sh.setContent(MimeType.TEXT, statusMessage.getBytes(StandardCharsets.UTF_8));
		return sh;
	}

	private boolean processCorsPreflight(HttpRequestContext context) {
		HttpClientHeader ch = context.getClientHeader();
		HttpServerHeader sh = context.getServerHeader();
		CorsConfiguration config = connection.getServer().getConfiguration().getDefaultCorsConfiguration(); // TODO: allow per path (pattern) CORS configuration
		if(config == null) return true;

		if(ch.getMethod() == HttpRequestMethod.OPTIONS) {
			String origin = ch.getFields().getFirst("Origin");
			if(origin == null) return true; // TODO: handle non-preflight OPTIONS requests

			// Ignoring Access-Control-Request-Method
			// Ignoring Access-Control-Request-Headers

			sh.setStatusCode(HttpStatusCodes.NO_CONTENT_204);

			if(config.isAllowAllOrigins() || config.getAllowedOrigins().contains(origin)) {
				sh.getFields().set("Access-Control-Allow-Origin", origin);
			}else if(config.getAllowedOrigins().contains("*")) {
				sh.getFields().set("Access-Control-Allow-Origin", "*");
			}

			Set<HttpRequestMethod> allowedMethods = new HashSet<>(config.getAllowedMethods());
			if(!config.isSendAllAllowedMethods()) allowedMethods.retainAll(connection.getServer().getDocumentProvider().getOptions(ch.getPath().getDocumentPath()));
			if(!allowedMethods.isEmpty()) {
				sh.getFields().set("Access-Control-Allow-Methods", allowedMethods.stream().map(m -> m.name()).collect(Collectors.joining(", ")));
			}

			sh.getFields().set("Access-Control-Max-Age", String.valueOf(config.getMaxAge()));
			if(!config.getExposedHeaders().isEmpty()) sh.getFields().set("Access-Control-Expose-Headers", config.getExposedHeaders().stream().collect(Collectors.joining(", ")));
			if(config.isAllowCredentials()) sh.getFields().set("Access-Control-Allow-Credentials", "true");
			if(!config.getAllowedHeaders().isEmpty()) sh.getFields().set("Access-Control-Allow-Headers", config.getAllowedHeaders().stream().collect(Collectors.joining(", ")));

			return false;
		}

		return true;
	}

	private boolean processCors(HttpRequestContext context) {HttpClientHeader ch = context.getClientHeader();
		HttpServerHeader sh = context.getServerHeader();
		CorsConfiguration config = connection.getServer().getConfiguration().getDefaultCorsConfiguration(); // TODO: allow per path (pattern) CORS configuration
		if(config == null) return true;

		String origin = ch.getFields().getFirst("Origin");
		if(origin != null) {
			if(config.isAllowAllOrigins() || config.getAllowedOrigins().contains(origin)) {
				sh.getFields().set("Access-Control-Allow-Origin", origin);
			}else if(config.getAllowedOrigins().contains("*")) {
				sh.getFields().set("Access-Control-Allow-Origin", "*");
			}

			if(!config.getExposedHeaders().isEmpty()) sh.getFields().set("Access-Control-Expose-Headers", config.getExposedHeaders().stream().collect(Collectors.joining(", ")));
		}

		return true;
	}

	private boolean applyRanges(HttpRequestContext context) {
		HttpServerHeader sh = context.getServerHeader();

		Pattern byteRangePattern = Pattern.compile("bytes=(?<start>\\d+)?-(?<end>\\d+)?"); // Multipart range requests are not supported
		String range = context.getClientHeader().getFields().getFirst("Range");
		if(range != null) {
			Matcher m = byteRangePattern.matcher(range);
			if(m.matches()) {
				try {
					if(m.group("start") != null) {
						long start = Long.parseLong(m.group("start"));
						if(start >= sh.getContentLength()) throw new FriendlyException("Range");
						if(m.group("end") != null) {
							long end = Long.parseLong(m.group("end"));
							if(end >= sh.getContentLength() || end > start) throw new FriendlyException("Range");
							sh.setContentOffset(start);
							sh.setContentLength(end - start + 1);
						}else {
							sh.setContentOffset(start);
							sh.setContentLength(sh.getContentLength() - start);
						}
					}else if(m.group("end") != null) {
						long endOff = Long.parseLong(m.group("end"));
						if(endOff > sh.getContentLength()) throw new FriendlyException("Range");
						sh.setContentOffset(sh.getContentLength() - endOff);
						sh.setContentLength(endOff);
					}else {
						sh.setStatusCode(HttpStatusCodes.BAD_REQUEST_400);
						sh.setContent(MimeType.HTML, "<h1>400 Bad Request</h1>".getBytes(StandardCharsets.UTF_8));
						return true;
					}
					sh.setStatusCode(HttpStatusCodes.PARTIAL_CONTENT_206);
					sh.getFields().set("Content-Range", "bytes " + sh.getContentOffset() + "-" + (sh.getContentOffset() + sh.getContentLength() - 1) + "/" + sh.getTotalContentLength());
				}catch(NumberFormatException | FriendlyException e) {
					sh.setStatusCode(HttpStatusCodes.REQUESTED_RANGE_NOT_SATISFIABLE_416);
					sh.setContent(MimeType.HTML, "<h1>416 Requested Range Not Satisfiable</h1>".getBytes(StandardCharsets.UTF_8));
				}
			}
		}

		return true;
	}

	private boolean applyCompression(HttpRequestContext context) {
		HttpServerHeader sh = context.getServerHeader();

		if(context.getClientHeader().getFields().getFirst("Accept-Encoding") == null) return true;
		if(sh.getContentLength() == 0 || sh.getContentLength() > Integer.MAX_VALUE) return true;

		List<String> supCs = Arrays.stream(context.getClientHeader().getFields().getFirst("Accept-Encoding").split(","))
				.map(String::trim)
				.collect(Collectors.toList());
		HttpCompressionMethod comp = connection.getServer().getCompressionMethods().stream()
				.filter(c -> supCs.contains(c.getName()))
				.findFirst().orElse(null);
		if(comp != null) {
			try {
				InputStream content = sh.getContent();
				byte[] uncompressedContent = content.readNBytes((int) sh.getContentLength());
				sh.getFields().set("Content-Encoding", comp.getName());
				sh.setContent(comp.compress(uncompressedContent));
			}catch(IOException e) {
				throw new FriendlyException("Failed to apply compression to content", e);
			}
		}

		return true;
	}

	public void close() {
		if(websocketConnection != null && connection.isSocketAlive()) websocketConnection.sendCloseFrame(CloseFrame.GOING_AWAY, "Server shutting down");
	}

	private class RequestAndResponse {

		private HttpClientHeader request;
		private HttpServerHeader response;

		public RequestAndResponse(HttpClientHeader request) {
			this.request = request;
		}

	}

}
