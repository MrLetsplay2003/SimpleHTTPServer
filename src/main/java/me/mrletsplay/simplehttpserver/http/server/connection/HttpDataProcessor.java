package me.mrletsplay.simplehttpserver.http.server.connection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
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
import me.mrletsplay.simplehttpserver.util.BufferUtil;
import me.mrletsplay.simplehttpserver.util.RWBuffer;
import me.mrletsplay.simplenio.reader.ReaderInstance;

public class HttpDataProcessor {

	private HttpConnection connection;

	private WebSocketConnection websocketConnection;

	private LinkedBlockingQueue<RequestAndResponse> responseQueue;

	private ReaderInstance<HttpClientHeader> readerInstance;

	private RequestAndResponse currentResponse;

	private boolean close;

	public HttpDataProcessor(HttpConnection connection) {
		this.connection = connection;
		this.responseQueue = new LinkedBlockingQueue<>();
		this.readerInstance = HttpReaders.CLIENT_HEADER_READER.createInstance();
		readerInstance.onFinished(request -> {
			connection.getLogger().debug(String.format("Received HTTP request: %s %s %s", request.getMethod(), request.getPath(), request.getProtocolVersion()));
			readerInstance.reset();
			RequestAndResponse requestAndResponse = new RequestAndResponse(request);
			responseQueue.offer(requestAndResponse);
			connection.getServer().getExecutor().submit(() -> processRequest(requestAndResponse));
		});
	}

	public void setWebsocketConnection(WebSocketConnection websocketConnection) {
		this.websocketConnection = websocketConnection;
	}

	public WebSocketConnection getWebsocketConnection() {
		return websocketConnection;
	}

	public void readData(ByteBuffer buffer) throws IOException {
		if(connection.getWebsocketConnection() != null) {
			connection.getWebsocketConnection().readData(buffer);
		}

		while(buffer.hasRemaining()) {
			readerInstance.read(buffer);
		}
	}

	public void writeData(ByteBuffer buffer) throws IOException {
		if(currentResponse != null) {
			synchronized (currentResponse) {
				if(!currentResponse.buffer.hasRemaining() && currentResponse.done) {
					currentResponse.stopProcessing();
					currentResponse = null; // EOF detected
				}
			}
		}

		if(currentResponse == null) {
			if(close) {
				connection.close();
				return;
			}

			if(responseQueue.isEmpty()) {
				if(connection.getWebsocketConnection() != null) {
					connection.getWebsocketConnection().writeData(buffer);
				} else if (connection.isReadShutdown()) {
					connection.close();
				}

				return;
			}

			if(responseQueue.peek().response == null) {
				// Next response is not yet ready
				return;
			}

			RequestAndResponse requestAndResponse = responseQueue.poll();
			requestAndResponse.startProcessing();
			currentResponse = requestAndResponse;
		}

		// Write response data
		synchronized (currentResponse) {
			int copied = BufferUtil.copyAvailable(currentResponse.buffer.read(), buffer);
			connection.getLogger().trace("Transferred " + copied + " bytes");

			if(!currentResponse.done) {
				if(currentResponse.buffer.remaining() < currentResponse.buffer.capacity() / 2) {
					currentResponse.doProcessing();
				}

				if(currentResponse.buffer.remaining() == 0) {
					// Wait for new data to be processed
					connection.stopWriting();
				}
			} else {
				currentResponse.stopProcessing();
				close = "close".equals(currentResponse.request.getFields().getFirst("Connection"));
				// FIXME: send response header
			}
		}
	}

	public boolean isIdle() {
		return currentResponse == null && responseQueue.isEmpty();
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
		HttpClientHeader request = requestAndResponse.request;
		connection.getLogger().debug(String.format("Processing request: %s %s %s", request.getMethod(), request.getPath(), request.getProtocolVersion()));

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
		requestAndResponse.startProcessing();
		connection.getLogger().debug(String.format("Finished processing request: %s %s %s", request.getMethod(), request.getPath(), request.getProtocolVersion()));
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

					long skipped;
					try {
						skipped = sh.getContent().skip(sh.getContentOffset());
					} catch (IOException e) {
						throw new FriendlyException("Range skip");
					}

					if(skipped < sh.getContentOffset()) {
						connection.getLogger().warn("Could not skip to content offset (skipped " + skipped + " of " + sh.getContentOffset() + " bytes)");
					}

					sh.setStatusCode(HttpStatusCodes.PARTIAL_CONTENT_206);
					sh.getFields().set("Content-Range", "bytes " + skipped + "-" + (skipped + sh.getContentLength() - 1) + "/" + sh.getTotalContentLength());
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
		if(websocketConnection != null && connection.isSocketAlive() && !websocketConnection.isClosed()) {
			websocketConnection.sendCloseFrame(CloseFrame.GOING_AWAY, "Server shutting down");
		}

		if(currentResponse != null) {
			currentResponse.stopProcessing();
			currentResponse = null;
		}
	}

	private class RequestAndResponse {

		private static final int BUFFER_SIZE = 512 * 1024; // TODO: Magic value
		private static final int CONTENT_BUFFER_SIZE = 1024; // TODO: Magic value

		private HttpClientHeader request;
		private HttpServerHeader response;

		private InputStream contentInputStream;
		private byte[] contentBuffer;
		private Future<?> processingFuture;
		private RWBuffer buffer;
		private boolean done;

		public RequestAndResponse(HttpClientHeader request) {
			this.request = request;
		}

		private void doProcessing0() {
			try {
				while(true) {
					synchronized(this) {
						if(Thread.interrupted()) {
							processingFuture = null;
							connection.getLogger().trace("Processing stopped: Interrupted");
							return;
						}

						buffer.flip();
						int spaceRemaining = buffer.remaining();
						buffer.flip();

						if(spaceRemaining < CONTENT_BUFFER_SIZE) {
							processingFuture = null;
							connection.startWriting();
							connection.getLogger().trace("Processing stopped: No space");
							return;
						}
					}

					int len = contentInputStream.read(contentBuffer);

					synchronized (this) {
						if(len == -1) {
							done = true;
							processingFuture = null;
							contentInputStream.close();
							connection.getLogger().trace("Processing stopped: EOF");
							connection.startWriting();
							return;
						}

						buffer.flip();
						buffer.write().put(contentBuffer, 0, len);
						buffer.flip();
						connection.startWriting();
					}
				}
			} catch (IOException e) {
				connection.getLogger().debug("Exception when processing response", e);
			}
		}

		public synchronized void doProcessing() {
			connection.getLogger().trace("Processing requested for " + request.getPath());
			if(processingFuture != null) {
				connection.getLogger().trace("Processing already running");
				return;
			}

			connection.getLogger().trace("Processing started");
			processingFuture = connection.getServer().getExecutor().submit(this::doProcessing0);
		}

		public synchronized void startProcessing() {
			done = false;
			buffer = RWBuffer.readableBuffer(BUFFER_SIZE);
			contentBuffer = new byte[CONTENT_BUFFER_SIZE];

			ByteArrayInputStream bIn = new ByteArrayInputStream(response.getHeaderBytes());
			contentInputStream = new SequenceInputStream(bIn, response.getContent());

			// TODO: only process when needed, don't stay on executor when waiting for reading to be done
			// -> increase buffer size and run processing on executor when there's too little data
		}

		public synchronized void stopProcessing() {
			if(processingFuture == null) return;
			processingFuture.cancel(true);
			processingFuture = null;
		}

		@Override
		public String toString() {
			return request.getPath().toString();
		}

	}

}
