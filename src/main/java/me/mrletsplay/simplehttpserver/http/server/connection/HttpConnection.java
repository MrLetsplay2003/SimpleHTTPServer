package me.mrletsplay.simplehttpserver.http.server.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.SSLSocket;

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
import me.mrletsplay.simplehttpserver.http.util.MimeType;
import me.mrletsplay.simplehttpserver.http.websocket.WebSocketConnection;
import me.mrletsplay.simplehttpserver.http.websocket.frame.CloseFrame;
import me.mrletsplay.simplehttpserver.server.ServerException;
import me.mrletsplay.simplehttpserver.server.connection.AbstractConnection;

public class HttpConnection extends AbstractConnection {

	private WebSocketConnection websocketConnection;

	public HttpConnection(HttpServer server, Socket socket) {
		super(server, socket);
		try {
			socket.setSoTimeout(10000);
		} catch (SocketException e) {
			getServer().getLogger().error("Error while intializing connection", e);
		}
	}

	public boolean isSecure() {
		return getSocket() instanceof SSLSocket;
	}

	@Override
	public void startRecieving() {
		getServer().getExecutor().submit(() -> {
			while(isSocketAlive() && !getServer().getExecutor().isShutdown()) {
				try {
					if(!receive()) {
						close();
						return;
					}
				}catch(SocketTimeoutException ignored) {
				}catch(SocketException ignored) {
					// Client probably just disconnected
					close();
				}catch(Exception e) {
					close();
					getServer().getLogger().error("Error in client receive loop", e);
					throw new ServerException("Error in client receive loop", e);
				}
			}
		});
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

	private boolean receive() throws IOException {
		if(websocketConnection != null) {
			websocketConnection.receive();
			return true;
		}

		HttpServerHeader sh = null;

		HttpClientHeader h = HttpClientHeader.parse(getSocket().getInputStream());
		if(h == null) return false;

		if(sh == null) sh = createResponse(h);

		boolean keepAlive = h != null && !"close".equalsIgnoreCase(h.getFields().getFirst("Connection"));
		if(keepAlive && !sh.getFields().has("Connection")) sh.getFields().set("Connection", "keep-alive");

		InputStream sIn = getSocket().getInputStream();
		OutputStream sOut = getSocket().getOutputStream();

		sOut.write(sh.getHeaderBytes());
		sOut.flush();

		InputStream in = sh.getContent();
		long skipped = in.skip(sh.getContentOffset());
		if(skipped < sh.getContentOffset()) getServer().getLogger().warn("Could not skip to content offset (skipped " + skipped + " of " + sh.getContentOffset() + " bytes)");

		byte[] buf = new byte[4096];
		int len;
		int tot = 0;
		while(sIn.available() == 0 && tot < sh.getContentLength() && (len = in.read(buf, 0, (int) Math.min(buf.length, sh.getContentLength() - tot))) > 0) {
			tot += len;
			sOut.write(buf, 0, len);
		}

		sOut.flush();

		HttpRequestContext.setCurrentContext(null);

		return keepAlive || websocketConnection != null;
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
		if(websocketConnection != null && isSocketAlive()) websocketConnection.sendCloseFrame(CloseFrame.GOING_AWAY, "Server shutting down");
		super.close();
	}

}
