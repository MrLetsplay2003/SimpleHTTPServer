package me.mrletsplay.simplehttpserver.http.websocket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import me.mrletsplay.simplehttpserver.http.reader.WebSocketReaders;
import me.mrletsplay.simplehttpserver.http.server.connection.HttpConnection;
import me.mrletsplay.simplehttpserver.http.websocket.frame.BinaryFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.CloseFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.PongFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.TextFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketOpCode;
import me.mrletsplay.simplenio.reader.ReaderInstance;

public class WebSocketConnection {

	private HttpConnection httpConnection;
	private WebSocketEndpoint endpoint;

	private boolean closed;
	private boolean closedByMe;
	private boolean closeFrameEnqueued, isSendingCloseFrame;

	private Object attachment;

	private ReaderInstance<WebSocketFrame> readerInstance;
	private WebSocketFrame incompleteFrame;

	private LinkedBlockingQueue<WebSocketFrame> outgoingQueue;
	private ReadableByteChannel currentOutgoingFrame;

	private LinkedBlockingQueue<WebSocketFrame> processingQueue;

	public WebSocketConnection(HttpConnection httpConnection, WebSocketEndpoint endpoint) {
		this.httpConnection = httpConnection;
		this.endpoint = endpoint;
		this.readerInstance = WebSocketReaders.FRAME_READER.createInstance();
		this.outgoingQueue = new LinkedBlockingQueue<>();
		this.processingQueue = new LinkedBlockingQueue<>();

		readerInstance.onFinished(frame -> {
			httpConnection.getLogger().debug(String.format("Received WebSocket frame: %s (fin: %s)", frame.getOpCode(), frame.isFin()));
			readerInstance.reset();
			processingQueue.offer(frame);
		});

		httpConnection.getServer().getExecutor().submit(this::handleFrames);
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
		if(closeFrameEnqueued) return;
		closeFrameEnqueued = true;

		if(reason == null) {
			send(CloseFrame.of(code));
		}else {
			send(CloseFrame.of(code, reason));
		}
	}

	public void close(int code, String reason, boolean closedByMe) {
		sendCloseFrame(code, reason);
		this.closedByMe = closedByMe;
		closeFrameEnqueued = true;
	}

	public void close(int code, String reason) {
		close(code, reason, true);
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

	public void readData(ByteBuffer buffer) throws IOException {
		while(buffer.hasRemaining()) {
			readerInstance.read(buffer);
		}
	}

	public void writeData(ByteBuffer buffer) throws IOException {
		if(currentOutgoingFrame == null) {
			if(isSendingCloseFrame) {
				if(!closedByMe) httpConnection.close();
				return;
			}

			if(outgoingQueue.isEmpty()) return;
		}

		if(currentOutgoingFrame == null) {
			WebSocketFrame outgoing = outgoingQueue.poll();
			if(outgoing instanceof CloseFrame) {
				isSendingCloseFrame = true;
			}

			ByteArrayOutputStream bOut = new ByteArrayOutputStream();
			outgoing.write(bOut);
			currentOutgoingFrame = Channels.newChannel(new ByteArrayInputStream(bOut.toByteArray()));
		}

		while(buffer.hasRemaining()) {
			if(currentOutgoingFrame.read(buffer) == -1) {
				currentOutgoingFrame = null;
				break;
			}
		}
	}

	private void handleFrames() {
		while(!isClosed() && httpConnection.isSocketAlive() && !httpConnection.getServer().getExecutor().isShutdown()) {
			try {
				WebSocketFrame frame = processingQueue.poll(1, TimeUnit.SECONDS);
				if(frame == null) continue;
				handleFrame(frame);
			} catch (InterruptedException e) {
				continue;
			}
		}
	}

	private void handleFrame(WebSocketFrame frame) {
		endpoint.onFrameReceived(this, frame);

		if(frame.isRSV1() || frame.isRSV2() || frame.isRSV3()) {
			close(CloseFrame.PROTOCOL_ERROR, "RSV1/2/3 must not be set");
			return;
		}

		if(frame.getOpCode().isControl()) {
			switch(frame.getOpCode()) {
				case CONNECTION_CLOSE:
					CloseFrame close = (CloseFrame) frame;

					if(close.getReceivedCode() != null && !CloseFrame.isCodeValid(close.getReceivedCode())) {
						close(CloseFrame.PROTOCOL_ERROR, "Invalid close code", false);
						return;
					}

					if(closedByMe) {
						forceClose(close);
						return;
					}

					close(CloseFrame.NORMAL_CLOSURE, null, false);
					return;
				case PING:
					endpoint.onPing(this, frame.getPayload());
					send(new PongFrame(true, false, false, false, frame.getPayload()));
					return;
				case PONG:
					endpoint.onPong(this, frame.getPayload());
					return;
				default:
					close(CloseFrame.PROTOCOL_ERROR, "Unsupported control frame: " + frame.getOpCode());
					return;
			}
		}

		if(incompleteFrame != null) {
			if(frame.getOpCode() != WebSocketOpCode.CONTINUATION_FRAME) {
				close(CloseFrame.PROTOCOL_ERROR, "Interleaving of continuation and non-continuation frames is not allowed");
				return;
			}

			incompleteFrame.appendPayload(frame.getPayload(), frame.isFin());
			if(frame.isFin()) {
				handleCompleteFrame(incompleteFrame);
				incompleteFrame = null;
			}

			return;
		}

		if(frame.getOpCode() == WebSocketOpCode.CONTINUATION_FRAME) {
			close(CloseFrame.PROTOCOL_ERROR, "Received continuation frame without prior incomplete frame");
			return;
		}

		if(!frame.isFin()) {
			incompleteFrame = frame;
			return;
		}

		handleCompleteFrame(frame);
	}

	private void handleCompleteFrame(WebSocketFrame frame) {
		try {
			frame.validatePayload();
		}catch(WebSocketException e) {
			close(CloseFrame.INVALID_FRAME_PAYLOAD_DATA, e.getMessage() != null ? e.getMessage() : "Failed to validate payload");
			return;
		}

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
