package me.mrletsplay.simplehttpserver;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import me.mrletsplay.simplehttpserver.http.reader.WebSocketReaders;
import me.mrletsplay.simplehttpserver.http.websocket.frame.TextFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketOpCode;
import me.mrletsplay.simplehttpserver.reader.ReaderInstance;

public class WebSocketReadersTest {

	private byte[] make(byte... bytes) {
		return bytes;
	}

	private byte[] make(byte[]... arrays) {
		if(arrays.length == 0) return new byte[0];

		byte[] bytes = new byte[Arrays.stream(arrays).mapToInt(a -> a.length).sum()];
		int i = 0;
		for(byte[] a : arrays) {
			System.arraycopy(a, 0, bytes, i, a.length);
			i += a.length;
		}

		return bytes;
	}

	@Test
	public void testFrameReaderSimple() {
		byte[] bytes = make(
			make((byte) 0b10000001, (byte) 0b10001000),
			make((byte) 0, (byte) 0, (byte) 0, (byte) 0),
			"testtest".getBytes(StandardCharsets.UTF_8)
		);

		ReaderInstance<WebSocketFrame> reader = WebSocketReaders.FRAME_READER.createInstance();
		assertTrue(assertDoesNotThrow(() -> reader.read(ByteBuffer.wrap(bytes))), "Reader did not finish reading");

		WebSocketFrame frame = reader.get();
		assertEquals(WebSocketOpCode.TEXT_FRAME, frame.getOpCode());
		assertEquals("testtest", ((TextFrame) frame).getText());
	}

}
