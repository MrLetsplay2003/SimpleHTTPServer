package me.mrletsplay.simplehttpserver.http.reader;

import me.mrletsplay.simplehttpserver.http.websocket.frame.BinaryFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.CloseFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.ContinuationFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.InvalidFrameException;
import me.mrletsplay.simplehttpserver.http.websocket.frame.PingFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.PongFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.TextFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketOpCode;
import me.mrletsplay.simplenio.reader.Operation;
import me.mrletsplay.simplenio.reader.Operations;
import me.mrletsplay.simplenio.reader.Reader;
import me.mrletsplay.simplenio.reader.ReaderImpl;
import me.mrletsplay.simplenio.reader.Ref;
import me.mrletsplay.simplenio.reader.SimpleRef;

public class WebSocketReaders {

	public static final Reader<WebSocketFrame> FRAME_READER = frameReader();

	private WebSocketReaders() {}

	private static Reader<WebSocketFrame> frameReader() {
		Reader<WebSocketFrame> reader = new ReaderImpl<>();
		Ref<Byte> b1 = reader.readByte();
		Ref<Boolean> finRef = b1.map(b -> (b & 0x80) != 0);
		Ref<Boolean> rsv1Ref = b1.map(b -> (b & 0x40) != 0);
		Ref<Boolean> rsv2Ref = b1.map(b -> (b & 0x20) != 0);
		Ref<Boolean> rsv3Ref = b1.map(b -> (b & 0x10) != 0);
		Ref<Integer> rawOpCode = b1.map(b -> b & 0x0F);
		Ref<WebSocketOpCode> opCode = rawOpCode.map(WebSocketOpCode::getByCode);

		reader.expect(instance -> opCode.get(instance) != WebSocketOpCode.UNKNOWN)
			.orElseRun(instance -> { throw new InvalidFrameException("Unknown opcode: " + rawOpCode.get(instance)); });

		reader.expect(instance -> !opCode.get(instance).isControl() || finRef.get(instance))
			.orElseRun(instance -> { throw new InvalidFrameException("Control frames can't be fragmented"); });

		Ref<Byte> b2 = reader.readByte();
		Ref<Boolean> mask = b2.map(b -> (b & 0x80) != 0);
		reader.expect(mask).orElseRun(() -> { throw new InvalidFrameException("Client-to-server frames must be masked"); });

		SimpleRef<Integer> fullPayloadLength = SimpleRef.create();
		Ref<Integer> payloadLength = b2.map(b -> b & 0x7F);

		reader.branch(payloadLength.map(l -> l < 126), Operations.run(instance -> fullPayloadLength.set(instance, payloadLength.get(instance))), null);
		reader.branch(payloadLength.map(l -> l == 126), readNByteLength(fullPayloadLength, 2), null);
		reader.branch(payloadLength.map(l -> l == 127), readNByteLength(fullPayloadLength, 8), null);

		Ref<byte[]> maskingKey = reader.readNBytes(4);
		Ref<byte[]> payload = reader.readNBytes(fullPayloadLength);

		reader.setConverter(instance -> {
			byte[] pl = payload.get(instance);
			byte[] mk = maskingKey.get(instance);

			for(int i = 0; i < pl.length; i++) {
				pl[i] = (byte) (pl[i] ^ mk[i % 4]);
			}

			boolean fin = finRef.get(instance);
			boolean rsv1 = rsv1Ref.get(instance);
			boolean rsv2 = rsv2Ref.get(instance);
			boolean rsv3 = rsv3Ref.get(instance);
			switch(opCode.get(instance)) {
				case BINARY_FRAME:
					return new BinaryFrame(fin, rsv1, rsv2, rsv3, pl);
				case CONNECTION_CLOSE:
					return new CloseFrame(fin, rsv1, rsv2, rsv3, pl);
				case CONTINUATION_FRAME:
					return new ContinuationFrame(fin, rsv1, rsv2, rsv3, pl);
				case PING:
					return new PingFrame(fin, rsv1, rsv2, rsv3, pl);
				case PONG:
					return new PongFrame(fin, rsv1, rsv2, rsv3, pl);
				case TEXT_FRAME:
					return new TextFrame(fin, rsv1, rsv2, rsv3, pl);
				default:
					throw new InvalidFrameException("Unsupported opcode: " + opCode);
			}
		});

		return reader;
	}

	private static Operation readNByteLength(SimpleRef<Integer> ref, int n) {
		SimpleRef<byte[]> bytes = SimpleRef.create();

		return Operations.allOf(
			Operations.readNBytes(bytes, n),
			Operations.run(instance -> {
				byte[] bs = bytes.get(instance);
				long value = 0;
				for(int i = 0; i < n; i++) {
					value |= ((long) (bs[i] & 0xFF)) << ((n - i - 1) * 8);
				}

				if(value < 0 || value > Integer.MAX_VALUE) throw new InvalidFrameException("Frame too big: " + value + " > " + Integer.MAX_VALUE);

				ref.set(instance, (int) value);
			})
		);
	}

}
