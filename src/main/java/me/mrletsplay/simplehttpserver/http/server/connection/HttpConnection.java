package me.mrletsplay.simplehttpserver.http.server.connection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import me.mrletsplay.mrcore.misc.FriendlyException;
import me.mrletsplay.simplehttpserver.http.HttpStatusCodes;
import me.mrletsplay.simplehttpserver.http.compression.HttpCompressionMethod;
import me.mrletsplay.simplehttpserver.http.document.HttpDocument;
import me.mrletsplay.simplehttpserver.http.exception.HttpResponseException;
import me.mrletsplay.simplehttpserver.http.header.HttpClientHeader;
import me.mrletsplay.simplehttpserver.http.header.HttpHeaderFields;
import me.mrletsplay.simplehttpserver.http.header.HttpServerHeader;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.server.HttpServer;
import me.mrletsplay.simplehttpserver.http.server.connection.buffer.RequestBuffer;
import me.mrletsplay.simplehttpserver.http.server.connection.buffer.ResponseBuffer;
import me.mrletsplay.simplehttpserver.http.util.MimeType;
import me.mrletsplay.simplehttpserver.http.websocket.WebSocketConnection;
import me.mrletsplay.simplehttpserver.http.websocket.frame.CloseFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.InvalidFrameException;
import me.mrletsplay.simplehttpserver.server.connection.AbstractConnection;

public class HttpConnection extends AbstractConnection {

	public static final int DEFAULT_MAX_CLIENT_HEADER_SIZE = 16384; // TODO: configurable

	private WebSocketConnection websocketConnection;

	private RequestBuffer requestBuffer;
	private ResponseBuffer responseBuffer;

	private ByteBuffer readBuffer;

	private boolean closeAfterWrite;

	public HttpConnection(HttpServer server, SocketChannel socket) {
		super(server, socket);

		this.requestBuffer = new RequestBuffer();
		this.responseBuffer = new ResponseBuffer();
		this.readBuffer = ByteBuffer.allocate(DEFAULT_MAX_CLIENT_HEADER_SIZE);
	}

	public boolean isSecure() {
//		return getSocket() instanceof SSLSocket;
		return false; // TODO
	}

	@Override
	public void readData() throws IOException {
		if(readBuffer.remaining() == 0) throw new IOException("Buffer is full");
		if(getSocket().read(readBuffer) == -1) {
			close();
			return;
		}

		readBuffer.flip();

		while(readBuffer.hasRemaining()) {
			int before = readBuffer.position();

			if(websocketConnection != null) {
				try {
					websocketConnection.readData(readBuffer);
				}catch(InvalidFrameException e) {
					close();
					break;
				}
			}else {
				try {
					requestBuffer.readData(readBuffer);
				}catch(HttpResponseException e) {
					respondNow(createResponseFromException(e));
					closeAfterWrite = true;
					break;
				}

				if(requestBuffer.isComplete()) {
					HttpClientHeader header = requestBuffer.getHeader();
					requestBuffer.clear();
					processRequest(header);
					break; // Don't continue reading data to avoid issues with multiple requests being processed at the same time
				}
			}

			if(readBuffer.position() == before) break; // Nothing has been read this cycle, wait for more data from client
		}

		readBuffer.compact();
	}

	@Override
	public void writeData() throws IOException {
		if(websocketConnection != null && responseBuffer.isComplete()) {
			websocketConnection.writeData(getSocket());
		}else {
			responseBuffer.writeData(getSocket());

			if(responseBuffer.isComplete()) {
				responseBuffer.clear();

				if(closeAfterWrite) {
					close();
					return;
				}

				// Response has been written, go back to waiting for a request
				getSelectionKey().interestOps(SelectionKey.OP_READ);
			}
		}
	}

	private void processRequest(HttpClientHeader request) {
		// While processing the request, we're not interested in new data or whether we can write
		getSelectionKey().interestOps(0);

		getServer().getExecutor().submit(() -> {
			try {
				HttpServerHeader response;

				try {
					response = createResponse(request);
				} catch(HttpResponseException e) {
					response = createResponseFromException(e);
				}

				boolean keepAlive = !"close".equalsIgnoreCase(request.getFields().getFirst("Connection"));
				if(keepAlive && !response.getFields().has("Connection")) response.getFields().set("Connection", "keep-alive");
				closeAfterWrite = !keepAlive;

				respondNow(response);
			} catch(Exception e) {
				getServer().getLogger().error("Error while processing request", e);
				close();
			}
		});
	}

	private void respondNow(HttpServerHeader response) throws IOException {
		responseBuffer.init(response);

		// Write the response back to the client
		getSelectionKey().interestOps(SelectionKey.OP_WRITE);
		getServer().getSelector().wakeup();
	}

	@Override
	public HttpServer getServer() {
		return (HttpServer) super.getServer();
	}

	public void setWebsocketConnection(WebSocketConnection websocketConnection) {
		this.websocketConnection = websocketConnection;
	}

	public WebSocketConnection getWebsocketConnection() {
		return websocketConnection;
	}

	private HttpServerHeader createResponse(HttpClientHeader h) {
		HttpServerHeader sh = new HttpServerHeader(getServer().getProtocolVersion(), HttpStatusCodes.OK_200, new HttpHeaderFields());
		HttpRequestContext ctx = new HttpRequestContext(this, h, sh);
		HttpRequestContext.setCurrentContext(ctx);

		HttpDocument d = getServer().getDocumentProvider().get(h.getMethod(), h.getPath().getDocumentPath());
		if(d == null) d = getServer().getDocumentProvider().getNotFoundDocument();

		try {
			try {
				d.createContent();

				sh = ctx.getServerHeader();
			}catch(HttpResponseException e) {
				sh = createResponseFromException(e);
			}

			if(sh.isAllowByteRanges()) applyRanges(sh);
			if(sh.isCompressionEnabled()) applyCompression(sh);
		}catch(Exception e) {
			getServer().getLogger().error("Error while creating page content", e);

			// Reset all of the context-related fields to ensure a clean environment
			sh = new HttpServerHeader(getServer().getProtocolVersion(), HttpStatusCodes.OK_200, new HttpHeaderFields());
			ctx = new HttpRequestContext(this, h, sh);
			HttpRequestContext.setCurrentContext(ctx);
			ctx.setException(e);

			getServer().getDocumentProvider().getErrorDocument().createContent();
		}

		return sh;
	}

	private HttpServerHeader createResponseFromException(HttpResponseException exception) {
		HttpServerHeader sh = new HttpServerHeader(getServer().getProtocolVersion(), exception.getStatusCode(), new HttpHeaderFields());
		String statusMessage = exception.getStatusMessage();
		if(statusMessage == null) statusMessage = "Error " + exception.getStatusCode().getStatusCode();
		sh.setContent(MimeType.TEXT, statusMessage.getBytes(StandardCharsets.UTF_8));
		return sh;
	}

	private void applyRanges(HttpServerHeader sh) {
		HttpRequestContext ctx = HttpRequestContext.getCurrentContext();

		Pattern byteRangePattern = Pattern.compile("bytes=(?<start>\\d+)?-(?<end>\\d+)?"); // Multipart range requests are not supported
		String range = ctx.getClientHeader().getFields().getFirst("Range");
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
						return;
					}
					sh.setStatusCode(HttpStatusCodes.PARTIAL_CONTENT_206);
					sh.getFields().set("Content-Range", "bytes " + sh.getContentOffset() + "-" + (sh.getContentOffset() + sh.getContentLength() - 1) + "/" + sh.getTotalContentLength());
				}catch(NumberFormatException | FriendlyException e) {
					sh.setStatusCode(HttpStatusCodes.REQUESTED_RANGE_NOT_SATISFIABLE_416);
					sh.setContent(MimeType.HTML, "<h1>416 Requested Range Not Satisfiable</h1>".getBytes(StandardCharsets.UTF_8));
				}
			}
		}
	}

	private void applyCompression(HttpServerHeader sh) throws IOException {
		HttpRequestContext ctx = HttpRequestContext.getCurrentContext();

		if(ctx.getClientHeader().getFields().getFirst("Accept-Encoding") == null) return;
		if(sh.getContentLength() == 0 || sh.getContentLength() > Integer.MAX_VALUE) return;

		List<String> supCs = Arrays.stream(ctx.getClientHeader().getFields().getFirst("Accept-Encoding").split(","))
				.map(String::trim)
				.collect(Collectors.toList());
		HttpCompressionMethod comp = getServer().getCompressionMethods().stream()
				.filter(c -> supCs.contains(c.getName()))
				.findFirst().orElse(null);
		if(comp != null) {
			InputStream content = sh.getContent();
			byte[] uncompressedContent = content.readNBytes((int) sh.getContentLength());
			sh.getFields().set("Content-Encoding", comp.getName());
			sh.setContent(comp.compress(uncompressedContent));
		}
	}

	@Override
	public void close() {
		if(websocketConnection != null && !websocketConnection.isClosed() && isSocketAlive()) websocketConnection.close(CloseFrame.GOING_AWAY, "Server shutting down");
		super.close();
	}

}
