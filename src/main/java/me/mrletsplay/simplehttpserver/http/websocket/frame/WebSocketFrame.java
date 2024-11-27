package me.mrletsplay.simplehttpserver.http.websocket.frame;

import java.io.IOException;
import java.io.OutputStream;

import me.mrletsplay.simplehttpserver.http.websocket.WebSocketException;

public abstract class WebSocketFrame {

	protected static final int MAX_FRAME_SIZE = 65536;

	private boolean fin;

	private boolean
		rsv1,
		rsv2,
		rsv3;

	private WebSocketOpCode opCode;
	private byte[] payload;

	public WebSocketFrame(boolean fin, boolean rsv1, boolean rsv2, boolean rsv3, WebSocketOpCode opCode, byte[] payload) {
		this.fin = fin;
		this.rsv1 = rsv1;
		this.rsv2 = rsv2;
		this.rsv3 = rsv3;
		this.opCode = opCode;
		this.payload = payload;
	}

	public boolean isFin() {
		return fin;
	}

	public boolean isRSV1() {
		return rsv1;
	}

	public boolean isRSV2() {
		return rsv2;
	}

	public boolean isRSV3() {
		return rsv3;
	}

	public WebSocketOpCode getOpCode() {
		return opCode;
	}

	public byte[] getPayload() {
		return payload;
	}

	public boolean appendPayload(byte[] additionalPayload, boolean fin) {
		long newLength = (long) payload.length + additionalPayload.length;
		if(newLength > Integer.MAX_VALUE) throw new WebSocketException("Concatenated frame too big: " + newLength + " > " + Integer.MAX_VALUE);
		byte[] newPayload = new byte[(int) newLength];
		System.arraycopy(payload, 0, newPayload, 0, payload.length);
		System.arraycopy(additionalPayload, 0, newPayload, payload.length, additionalPayload.length);
		this.payload = newPayload;
		this.fin = fin;
		return true;
	}

	public void validatePayload() throws WebSocketException {
		// Override in relevant subclasses
	}

	public void write(OutputStream out) throws IOException {
		int b1 = (fin ? 1 : 0) << 7
				| (rsv1 ? 1 : 0) << 6
				| (rsv2 ? 1 : 0) << 5
				| (rsv3 ? 1 : 0) << 4
				| opCode.getCode();
		out.write(b1);

		if(payload.length > 65535) {
			out.write(127);
			out.write(0); // First four bytes are all 0, because max payload size is Integer.MAX_VALUE
			out.write(0);
			out.write(0);
			out.write(0);
			out.write(payload.length >> 24);
			out.write((payload.length >> 16) & 0xFF);
			out.write((payload.length >> 8) & 0xFF);
			out.write(payload.length & 0xFF);
		}else if(payload.length >= 126) {
			out.write(126);
			out.write((payload.length >> 8) & 0xFF);
			out.write(payload.length & 0xFF);
		}else {
			out.write(payload.length);
		}

		out.write(payload);
		out.flush();
	}

	public abstract WebSocketFrame[] split();

}
