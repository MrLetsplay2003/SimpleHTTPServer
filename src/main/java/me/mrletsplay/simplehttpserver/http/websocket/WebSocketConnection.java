package me.mrletsplay.simplehttpserver.http.websocket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;

import me.mrletsplay.simplehttpserver.http.reader.WebSocketReaders;
import me.mrletsplay.simplehttpserver.http.server.connection.HttpConnection;
import me.mrletsplay.simplehttpserver.http.websocket.frame.BinaryFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.CloseFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.PongFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.TextFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketOpCode;
import me.mrletsplay.simplehttpserver.reader.ReaderInstance;

public class WebSocketConnection {

	private HttpConnection httpConnection;
	private WebSocketEndpoint endpoint;

	private boolean hasSentCloseFrame;
	private boolean closed;

	private Object attachment;

	private ReaderInstance<WebSocketFrame> readerInstance;
	private WebSocketFrame incompleteFrame;

	private LinkedBlockingQueue<WebSocketFrame> outgoingQueue;
	private ReadableByteChannel currentOutgoingFrame;

	public WebSocketConnection(HttpConnection httpConnection, WebSocketEndpoint endpoint) {
		this.httpConnection = httpConnection;
		this.endpoint = endpoint;
		this.readerInstance = WebSocketReaders.FRAME_READER.createInstance();
		this.outgoingQueue = new LinkedBlockingQueue<>();
		readerInstance.onFinished(frame -> {
			httpConnection.getLogger().debug(String.format("Received WebSocket frame: %s (fin: %s)", frame.getOpCode(), frame.isFin()));
			readerInstance.reset();
			httpConnection.getServer().getExecutor().submit(() -> handleFrame(frame));
		});
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

	public void send(WebSocketFrame frame) {
		if(closed) throw new WebSocketException("Connection is closed");
		outgoingQueue.offer(frame);
		httpConnection.startWriting();
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

	public void readData(ByteBuffer buffer) throws IOException {
		readerInstance.read(buffer);
	}

	public void writeData(ByteBuffer buffer) throws IOException {
		if(currentOutgoingFrame == null && outgoingQueue.isEmpty()) {
			return;
		}

		if(currentOutgoingFrame == null) {
			WebSocketFrame outgoing = outgoingQueue.poll();
			ByteArrayOutputStream bOut = new ByteArrayOutputStream();
			outgoing.write(bOut);
			currentOutgoingFrame = Channels.newChannel(new ByteArrayInputStream(bOut.toByteArray()));
		}

		if(currentOutgoingFrame.read(buffer) == -1) {
			currentOutgoingFrame = null;
		}
	}

	private void handleFrame(WebSocketFrame frame) {
		endpoint.onFrameReceived(this, frame);

		if(frame.getOpCode().isControl()) {
			switch(frame.getOpCode()) {
				case CONNECTION_CLOSE:
					if(hasSentCloseFrame) {
						forceClose((CloseFrame) frame);
						return;
					}

					close();
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
