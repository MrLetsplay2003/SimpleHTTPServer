package me.mrletsplay.simplehttpserver.http.websocket.buffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import me.mrletsplay.simplehttpserver.http.websocket.frame.BinaryFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.CloseFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.ContinuationFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.InvalidFrameException;
import me.mrletsplay.simplehttpserver.http.websocket.frame.PingFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.PongFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.TextFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketFrame;
import me.mrletsplay.simplehttpserver.http.websocket.frame.WebSocketOpCode;

public class IncomingFrameBuffer {

	private boolean fin, rsv1, rsv2, rsv3;
	private WebSocketOpCode opCode;
	private boolean readPayloadLength;
	private int payloadLength;
	private boolean maskingKeyRead;
	private byte[] maskingKey = new byte[4];
	private ByteArrayOutputStream payload;

	private WebSocketFrame frame;

	public void readData(ByteBuffer buffer) throws IOException {
		if(buffer.remaining() < 2
			|| (payloadLength == 126 && buffer.remaining() < 4)
			|| (payloadLength == 127 && buffer.remaining() < 8)
			|| (!maskingKeyRead && buffer.remaining() < maskingKey.length)) return;

		if(!readPayloadLength) {
			int b1 = buffer.get() & 0xFF;
			int b2 = buffer.get() & 0xFF;

			fin = (b1 & 0x80) != 0;
			rsv1 = (b1 & 0x40) != 0;
			rsv2 = (b1 & 0x20) != 0;
			rsv3 = (b1 & 0x10) != 0;

			int rawOpCode = b1 & 0x0F;
			opCode = WebSocketOpCode.getByCode(rawOpCode);
			if(opCode == WebSocketOpCode.UNKNOWN) throw new InvalidFrameException("Unknown opcode: " + rawOpCode);
			if(opCode.isControl() && !fin) throw new InvalidFrameException("Control frames can't be fragmented");

			boolean mask = (b2 & 0x80) != 0;
			if(!mask) throw new InvalidFrameException("Client-to-server frames must be masked");

			payloadLength = b2 & 0x7F;
			readPayloadLength = true;

			return;
		}

		if(payloadLength == 126) {
			payloadLength = buffer.getShort();
			if(payloadLength < 126) throw new InvalidFrameException("Payload length must be encoded with the minimal number of bytes");
			return;
		}

		if(payloadLength == 127) {
			long tempPayloadLength = buffer.getLong();
			if(payloadLength < 127) throw new InvalidFrameException("Payload length must be encoded with the minimal number of bytes");
			if(payloadLength < 0) throw new InvalidFrameException("Most significant bit must not be set");
			if(payloadLength > WebSocketFrame.MAX_FRAME_SIZE) throw new InvalidFrameException("Frame too big: " + payloadLength + " > " + WebSocketFrame.MAX_FRAME_SIZE);
			payloadLength = (int) tempPayloadLength;
			return;
		}

		if(!maskingKeyRead) {
			buffer.get(maskingKey);
			maskingKeyRead = true;
		}

		payload = new ByteArrayOutputStream(payloadLength);
		int toRead = Math.min(payloadLength - payload.size(), buffer.remaining());
		payload.write(buffer.array(), buffer.position(), toRead);
		buffer.position(buffer.position() + toRead);

		if(payload.size() == payloadLength) {
			byte[] payload = this.payload.toByteArray();
			for(int i = 0; i < payloadLength; i++) {
				payload[i] = (byte) (payload[i] ^ maskingKey[i % 4]);
			}

			switch(opCode) {
				case BINARY_FRAME:
					frame = new BinaryFrame(fin, rsv1, rsv2, rsv3, payload);
					break;
				case CONNECTION_CLOSE:
					frame = new CloseFrame(fin, rsv1, rsv2, rsv3, payload);
					break;
				case CONTINUATION_FRAME:
					frame = new ContinuationFrame(fin, rsv1, rsv2, rsv3, payload);
					break;
				case PING:
					frame = new PingFrame(fin, rsv1, rsv2, rsv3, payload);
					break;
				case PONG:
					frame = new PongFrame(fin, rsv1, rsv2, rsv3, payload);
					break;
				case TEXT_FRAME:
					frame = new TextFrame(fin, rsv1, rsv2, rsv3, payload);
					break;
				default:
					throw new InvalidFrameException("Unhandled opcode: " + opCode);
			}
		}
	}

	public boolean isComplete() {
		return frame != null;
	}

	public WebSocketFrame getFrame() {
		return frame;
	}

	public void clear() {
		fin = rsv1 = rsv2 = rsv3 = false;
		opCode = null;
		readPayloadLength = false;
		payloadLength = 0;
		maskingKeyRead = false;
		// TODO: Clear masking key?
		payload = null;
		frame = null;
	}

}
