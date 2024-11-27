package me.mrletsplay.simplehttpserver.http.websocket.frame;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;

import me.mrletsplay.simplehttpserver.http.websocket.WebSocketException;

public class TextFrame extends WebSocketFrame {

	private String text;

	public TextFrame(boolean fin, boolean rsv1, boolean rsv2, boolean rsv3, byte[] payload) {
		super(fin, rsv1, rsv2, rsv3, WebSocketOpCode.TEXT_FRAME, payload);
	}

	@Override
	public void validatePayload() throws WebSocketException {
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
		try {
			this.text = decoder.decode(ByteBuffer.wrap(getPayload())).toString();
		} catch (CharacterCodingException e) {
			throw new WebSocketException("Invalid UTF-8 in payload", e);
		}
	}

	public String getText() {
		return text;
	}

	@Override
	public WebSocketFrame[] split() {
		if(getPayload().length < MAX_FRAME_SIZE) return new WebSocketFrame[] {this};
		int count = (int) Math.ceil((float) getPayload().length / MAX_FRAME_SIZE);
		WebSocketFrame[] frames = new WebSocketFrame[count];
		byte[] payload = getPayload();
		byte[] firstPayload = new byte[MAX_FRAME_SIZE];
		System.arraycopy(payload, 0, firstPayload, 0, MAX_FRAME_SIZE);
		frames[0] = new TextFrame(false, false, false, false, firstPayload);
		for(int i = 1; i < count; i++) {
			int size = Math.min(MAX_FRAME_SIZE, payload.length - i * MAX_FRAME_SIZE);
			byte[] splPayload = new byte[size];
			System.arraycopy(payload, i * MAX_FRAME_SIZE, splPayload, 0, size);
			frames[i] = new ContinuationFrame(i == count - 1, isRSV1(), isRSV2(), isRSV3(), splPayload);
		}
		return frames;
	}

}
