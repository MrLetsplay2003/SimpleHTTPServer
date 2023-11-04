package me.mrletsplay.simplehttpserver.http.websocket.buffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketFrame;
import me.mrletsplay.simplehttpserver.util.BufferUtil;

public class OutgoingFrameBuffer {

	private Object framesLock = new Object();

	private List<WebSocketFrame> frames;
	private ByteBuffer currentFrame;

	public OutgoingFrameBuffer() {
		this.frames = new ArrayList<>();
	}

	public void sendFrame(WebSocketFrame frame) {
		synchronized (framesLock) {
			frames.addAll(Arrays.asList(frame.split()));
		}
	}

	public void writeData(ByteBuffer buffer) throws IOException {
		if(currentFrame == null) {
			synchronized (framesLock) {
				if(frames.isEmpty()) return;
				WebSocketFrame frame = frames.remove(0);
				ByteArrayOutputStream bOut = new ByteArrayOutputStream();
				frame.write(bOut);
				currentFrame = ByteBuffer.wrap(bOut.toByteArray());
			}
		}

		BufferUtil.copyAvailable(currentFrame, buffer);
		if(!currentFrame.hasRemaining()) currentFrame = null;
	}

	public boolean isComplete() {
		synchronized (framesLock) {
			return frames.isEmpty() && currentFrame == null;
		}
	}

}
