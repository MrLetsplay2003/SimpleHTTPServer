package me.mrletsplay.simplehttpserver.http.websocket;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import me.mrletsplay.simplehttpserver.http.server.connection.HttpConnectionImpl;
import me.mrletsplay.simplehttpserver.http.websocket.frame.BinaryFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.CloseFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.TextFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketFrame;

public class WebSocketConnection {

	private HttpConnectionImpl httpConnection;
	private WebSocketEndpoint endpoint;

	private boolean hasSentCloseFrame;
	private boolean closed;

	private WebSocketFrame incompleteFrame;

	private Object attachment;

	public WebSocketConnection(HttpConnectionImpl httpConnection, WebSocketEndpoint endpoint) {
		this.httpConnection = httpConnection;
		this.endpoint = endpoint;
	}

	public HttpConnectionImpl getHttpConnection() {
		return httpConnection;
	}

	public WebSocketEndpoint getEndpoint() {
		return endpoint;
	}

	public void setAttachment(Object attachment) {
		this.attachment = attachment;
	}

	@SuppressWarnings("unchecked")
	public <T> T getAttachment() {
		return (T) attachment;
	}

	public void send(WebSocketFrame frame) {
		if(closed) throw new WebSocketException("Connection is closed");
//		try {
//			for(WebSocketFrame f : frame.split()) { TODO
//				f.write(httpConnection.getSocket().getOutputStream());
//			}
//			httpConnection.getSocket().getOutputStream().flush();
//		}catch(IOException e) {
//			throw new WebSocketException("Failed to send frame", e);
//		}
	}

	public void sendText(String message) {
		send(new TextFrame(true, false, false, false, message.getBytes(StandardCharsets.UTF_8)));
	}

	public void sendBinary(byte[] bytes) {
		send(new BinaryFrame(true, false, false, false, bytes));
	}

	public void forceClose() {
		forceClose(null);
	}

	private void forceClose(CloseFrame frame) {
		endpoint.getConnections().remove(this);
		closed = true;
		httpConnection.close();
		endpoint.onClose(this, frame);
	}

	public void sendCloseFrame(int code, String reason) {
		if(reason == null) {
			send(CloseFrame.of(code));
		}else {
			send(CloseFrame.of(code, reason));
		}
	}

	public void close(int code, String reason) {
		sendCloseFrame(code, reason);
		hasSentCloseFrame = true;
	}

	public void close(int code) {
		close(code, null);
	}

	public void close() {
		close(1000);
	}

	public void receive() throws IOException {
//		WebSocketFrame frame; TODO
//		frame = WebSocketFrame.read(httpConnection.getSocket().getInputStream());
//		if(frame == null) {
//			// Mark connection as dead
//			httpConnection.setDead();
//			forceClose();
//			return;
//		}
//
//		endpoint.onFrameReceived(this, frame);
//
//		if(frame.getOpCode().isControl()) {
//			switch(frame.getOpCode()) {
//				case CONNECTION_CLOSE:
//					if(hasSentCloseFrame) {
//						forceClose((CloseFrame) frame);
//						return;
//					}
//
//					close();
//					return;
//				case PING:
//					endpoint.onPing(this, frame.getPayload());
//					send(new PongFrame(true, false, false, false, new byte[0]));
//					return;
//				case PONG:
//					endpoint.onPong(this, frame.getPayload());
//					return;
//				default:
//					throw new WebSocketException("Unsupported control frame: " + frame.getOpCode());
//			}
//		}
//
//		if(incompleteFrame != null) {
//			if(frame.getOpCode() != WebSocketOpCode.CONTINUATION_FRAME) throw new WebSocketException("Interleaving of message frames is not allowed");
//			incompleteFrame.appendPayload(frame.getPayload());
//			if(frame.isFin()) {
//				handleCompleteFrame(frame);
//				incompleteFrame = null;
//			}
//			return;
//		}
//
//		if(frame.getOpCode() == WebSocketOpCode.CONTINUATION_FRAME) throw new WebSocketException("Received continuation frame without prior incomplete frame");
//
//		if(!frame.isFin()) {
//			incompleteFrame = frame;
//			return;
//		}
//
//		handleCompleteFrame(frame);
	}

	private void handleCompleteFrame(WebSocketFrame frame) {
		endpoint.onCompleteFrameReceived(this, frame);

		switch(frame.getOpCode()) {
			case BINARY_FRAME:
				endpoint.onBinaryMessage(this, frame.getPayload());
				break;
			case TEXT_FRAME:
				endpoint.onTextMessage(this, ((TextFrame) frame).getText());
				break;
			default:
				break;
		}
	}

}
