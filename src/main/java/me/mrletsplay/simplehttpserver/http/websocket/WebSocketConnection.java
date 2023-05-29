package me.mrletsplay.simplehttpserver.http.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;

import me.mrletsplay.simplehttpserver.http.server.connection.HttpConnection;
import me.mrletsplay.simplehttpserver.http.websocket.buffer.IncomingFrameBuffer;
import me.mrletsplay.simplehttpserver.http.websocket.buffer.OutgoingFrameBuffer;
import me.mrletsplay.simplehttpserver.http.websocket.frame.BinaryFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.CloseFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.PongFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.TextFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketOpCode;

public class WebSocketConnection {

	// FIXME: onClose might not be called if the client disconnects without sending a close frame

	private HttpConnection httpConnection;
	private WebSocketEndpoint endpoint;

	private boolean hasSentCloseFrame;
	private boolean closed;
	private boolean closeAfterWrite;

	private WebSocketFrame incompleteFrame;

	private Object attachment;

	private IncomingFrameBuffer inBuffer;
	private OutgoingFrameBuffer outBuffer;

	public WebSocketConnection(HttpConnection httpConnection, WebSocketEndpoint endpoint) {
		this.httpConnection = httpConnection;
		this.endpoint = endpoint;
		this.inBuffer = new IncomingFrameBuffer();
		this.outBuffer = new OutgoingFrameBuffer();
	}

	public HttpConnection getHttpConnection() {
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

	public void readData(ByteBuffer buffer) throws IOException {
		inBuffer.readData(buffer); // TODO: handle InvalidFrameException

		if(inBuffer.isComplete()) {
			WebSocketFrame frame = inBuffer.getFrame();
			inBuffer.clear();
			processFrame(frame);
		}
	}

	private void processFrame(WebSocketFrame frame) {
		httpConnection.getServer().getExecutor().submit(() -> {
			endpoint.onFrameReceived(this, frame);

			if(frame.getOpCode().isControl()) {
				switch(frame.getOpCode()) {
					case CONNECTION_CLOSE:
						if(hasSentCloseFrame) {
							forceClose();
							return;
						}

						CloseFrame cf = (CloseFrame) frame;
						sendCloseFrame(cf.getCode(), cf.getReason());
						closeAfterWrite = true;
						return;
					case PING:
						endpoint.onPing(this, frame.getPayload());
						send(new PongFrame(true, false, false, false, new byte[0]));
						return;
					case PONG:
						endpoint.onPong(this, frame.getPayload());
						return;
					default:
						throw new WebSocketException("Unsupported control frame: " + frame.getOpCode());
				}
			}

			if(incompleteFrame != null) {
				if(frame.getOpCode() != WebSocketOpCode.CONTINUATION_FRAME) throw new WebSocketException("Interleaving of message frames is not allowed");
				incompleteFrame.appendPayload(frame.getPayload());
				if(frame.isFin()) {
					handleCompleteFrame(frame);
					incompleteFrame = null;
				}
				return;
			}

			if(frame.getOpCode() == WebSocketOpCode.CONTINUATION_FRAME) throw new WebSocketException("Received continuation frame without prior incomplete frame");

			if(!frame.isFin()) {
				incompleteFrame = frame;
				return;
			}

			handleCompleteFrame(frame);
		});
	}

	public void writeData(ByteChannel buffer) throws IOException {
		outBuffer.writeData(buffer);

		if(outBuffer.isComplete()) {
			if(closeAfterWrite) {
				forceClose();
				return;
			}

			httpConnection.getSelectionKey().interestOpsAnd(~SelectionKey.OP_WRITE);
		}
	}

	public void send(WebSocketFrame frame) {
		if(closed) throw new WebSocketException("Connection is closed");
		outBuffer.sendFrame(frame);

		httpConnection.getSelectionKey().interestOpsOr(SelectionKey.OP_WRITE);
		httpConnection.getServer().getSelector().wakeup();
	}

	public void sendText(String message) {
		send(new TextFrame(true, false, false, false, message.getBytes(StandardCharsets.UTF_8)));
	}

	public void sendBinary(byte[] bytes) {
		send(new BinaryFrame(true, false, false, false, bytes));
	}

	private void forceClose() {
		forceClose(null);
	}

	private void forceClose(CloseFrame frame) {
		endpoint.getConnections().remove(this);
		closed = true;
		httpConnection.close();
		endpoint.onClose(this, frame);
	}

	private void sendCloseFrame(int code, String reason) {
		if(closed) return;
		hasSentCloseFrame = true;

		if(reason == null) {
			send(CloseFrame.of(code));
		}else {
			send(CloseFrame.of(code, reason));
		}
	}

	public void close(int code, String reason) {
		sendCloseFrame(code, reason);
	}

	public void close(int code) {
		close(code, null);
	}

	public void close() {
		close(1000);
	}

	public boolean isClosed() {
		return closed;
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
