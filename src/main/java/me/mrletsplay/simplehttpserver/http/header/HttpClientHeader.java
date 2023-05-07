package me.mrletsplay.simplehttpserver.http.header;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Objects;

import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;
import me.mrletsplay.simplehttpserver.http.header.body.ChunkedRequestBody;
import me.mrletsplay.simplehttpserver.http.header.body.DefaultRequestBody;
import me.mrletsplay.simplehttpserver.http.header.body.HttpRequestBody;

public class HttpClientHeader {

	private HttpRequestMethod method;
	private HttpUrlPath path;
	private String protocolVersion;
	private HttpHeaderFields fields;
	private byte[] postData;

	private HttpRequestBody body;

	public HttpClientHeader(HttpRequestMethod method, HttpUrlPath path, String protocolVersion, HttpHeaderFields fields) {
		this.method = method;
		this.path = path;
		this.protocolVersion = protocolVersion;
		this.fields = fields;
	}

	public HttpRequestMethod getMethod() {
		return method;
	}

	public HttpUrlPath getPath() {
		return path;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public HttpHeaderFields getFields() {
		return fields;
	}

	public PostData getPostData() {
		if(!isBodyComplete()) throw new IllegalStateException("Body is not complete");
		return new PostData(fields, postData);
	}

	private void checkComplete() {
		if(body.isComplete()) {
			postData = body.toByteArray();
			body = null;
		}
	}

	public void readBody(ByteBuffer buffer) throws IOException {
		if(isBodyComplete()) throw new IllegalStateException("Body is complete");

		body.read(buffer);
		checkComplete();
	}

	public void readBody(ReadableByteChannel channel) throws IOException {
		if(isBodyComplete()) throw new IllegalStateException("Body is complete");

		body.read(channel);
		checkComplete();
	}

	public boolean isBodyComplete() {
		return postData != null;
	}

	public static HttpClientHeader parseHead(byte[] data, int offset, int length) {
		return parseHead(new ByteArrayInputStream(data, offset, length));
	}

	public static HttpClientHeader parseHead(InputStream data) {
		try {
			String reqLine = readLine(data);
			if(reqLine == null) return null;

			String[] fs = reqLine.split(" ");
			if(fs.length != 3) return null;

			HttpRequestMethod method = HttpRequestMethod.get(fs[0]);
			if(method == null) return null;

			HttpUrlPath path = HttpUrlPath.parse(fs[1]);
			if(path == null) return null;

			String protocolVersion = fs[2];
			HttpHeaderFields fields = parseHeaders(data);
			if(fields == null) return null;

			HttpClientHeader header = new HttpClientHeader(method, path, protocolVersion, fields);

			HttpRequestBody body = null;

			String cL = fields.getFirst("Content-Length");
			if(cL != null) {
				int contLength;
				try {
					contLength = Integer.parseInt(fields.getFirst("Content-Length"));
				}catch(Exception e) {
					return null;
				}

				if(contLength > 0) body = new DefaultRequestBody(contLength);
			}

			String encoding = fields.getFirst("Transfer-Encoding");
			if(encoding != null) {
				if(!encoding.equalsIgnoreCase("chunked")) return null; // Unsupported transfer encoding, TODO: deflate, gzip
				if(fields.has("Trailer")) return null; // Trailers are not supported
				body = new ChunkedRequestBody();
			}

			if(body != null) {
				header.body = body;
			}else {
				header.postData = new byte[0];
			}

			return header;
		}catch(IOException e) {
			return null;
		}
	}

	public static byte[] readChunks(InputStream in) throws IOException {
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();

		while(true) {
			String lenStr = readLine(in);
			int len = Integer.parseInt(lenStr, 16);
			if(len == 0) break;
			byte[] buf = new byte[len];
			if(in.read(buf) != len) return null;
			bOut.write(buf);
			if(in.read() != '\r') return null;
			if(in.read() != '\n') return null;
		}

		if(in.read() != '\r') return null;
		if(in.read() != '\n') return null;
		return bOut.toByteArray();
	}

	public static HttpHeaderFields parseHeaders(InputStream in) throws IOException {
		HttpHeaderFields fields = new HttpHeaderFields();
		String l;
		while((l = readLine(in)) != null && !l.isEmpty()) {
			if(l.isEmpty()) break;
			String[] kv = l.split(": ", 2);
			fields.add(kv[0], kv[1]);
		}
		if(l == null) {
			return null;
		}
		return fields;
	}

	private static String readLine(InputStream in) throws IOException {
		StringBuilder b = new StringBuilder();
		int c;
		while((c = in.read()) != '\r' && c != -1) {
			b.appendCodePoint(c);
		}
		if(c == -1) return null;
		if(in.read() != '\n') return null;
		return b.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof HttpClientHeader)) return false;
		HttpClientHeader o = (HttpClientHeader) obj;
		return method.equals(o.method)
				&& path.equals(o.path)
				&& protocolVersion.equals(o.protocolVersion)
				&& fields.equals(o.fields)
				&& Arrays.equals(postData, o.postData);
	}

	@Override
	public int hashCode() {
		return Objects.hash(method, path, protocolVersion, fields, Arrays.hashCode(postData));
	}

}
