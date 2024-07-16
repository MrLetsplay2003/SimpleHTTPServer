package me.mrletsplay.simplehttpserver.http.server.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import me.mrletsplay.simplehttpserver.http.server.HttpServer;
import me.mrletsplay.simplehttpserver.http.websocket.WebSocketConnection;
import me.mrletsplay.simplehttpserver.server.connection.AbstractBufferedConnection;

public class HttpConnectionImpl extends AbstractBufferedConnection implements HttpConnection {

	private HttpDataProcessor dataProcessor;

	public HttpConnectionImpl(HttpServer server, SelectionKey selectionKey, SocketChannel socket) {
		super(server, selectionKey, socket);
		this.dataProcessor = new HttpDataProcessor(this);
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public HttpServer getServer() {
		return (HttpServer) super.getServer();
	}

	@Override
	public void setWebsocketConnection(WebSocketConnection connection) {
		dataProcessor.setWebsocketConnection(connection);
	}

	@Override
	public WebSocketConnection getWebsocketConnection() {
		return dataProcessor.getWebsocketConnection();
	}

	@Override
	public void readData(ByteBuffer buffer) throws IOException {
		dataProcessor.readData(buffer);
	}

	@Override
	public void writeData(ByteBuffer buffer) throws IOException {
		dataProcessor.writeData(buffer);
	}

//	@Override
//	public void readData() throws IOException {
//		if(websocketConnection != null) {
//			websocketConnection.receive();
//			return true;
//		}
//
//		HttpServerHeader sh = null;
//
//		HttpClientHeader h = HttpClientHeader.parse(getSocket().getInputStream(), getServer().getConfiguration().getReadTimeout());
//		if(h == null) return false;
//
//		if(sh == null) sh = createResponse(h);
//
//		boolean keepAlive = h != null && !"close".equalsIgnoreCase(h.getFields().getFirst("Connection"));
//		if(keepAlive && !sh.getFields().has("Connection")) sh.getFields().set("Connection", "keep-alive");
//
//		InputStream sIn = getSocket().getInputStream();
//		OutputStream sOut = getSocket().getOutputStream();
//
//		sOut.write(sh.getHeaderBytes());
//		sOut.flush();
//
//		InputStream in = sh.getContent();
//		long skipped = in.skip(sh.getContentOffset());
//		if(skipped < sh.getContentOffset()) getServer().getLogger().warn("Could not skip to content offset (skipped " + skipped + " of " + sh.getContentOffset() + " bytes)");
//
//		byte[] buf = new byte[4096];
//		int len;
//		int tot = 0;
//		while(sIn.available() == 0 && tot < sh.getContentLength() && (len = in.read(buf, 0, (int) Math.min(buf.length, sh.getContentLength() - tot))) > 0) {
//			tot += len;
//			sOut.write(buf, 0, len);
//		}
//
//		sOut.flush();
//
//		HttpRequestContext.setCurrentContext(null);
//
//		return keepAlive || websocketConnection != null;
//	}

	@Override
	public void close() {
		// TODO: wait for queued responses to be sent before closing
		dataProcessor.close();
		super.close();
	}

}
