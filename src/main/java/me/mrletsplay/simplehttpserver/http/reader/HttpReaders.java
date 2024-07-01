package me.mrletsplay.simplehttpserver.http.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;
import me.mrletsplay.simplehttpserver.http.header.HttpClientHeader;
import me.mrletsplay.simplehttpserver.http.header.HttpHeaderFields;
import me.mrletsplay.simplehttpserver.http.header.HttpUrlPath;
import me.mrletsplay.simplehttpserver.reader.Operation;
import me.mrletsplay.simplehttpserver.reader.Operations;
import me.mrletsplay.simplehttpserver.reader.Reader;
import me.mrletsplay.simplehttpserver.reader.ReaderImpl;
import me.mrletsplay.simplehttpserver.reader.Ref;
import me.mrletsplay.simplehttpserver.reader.SimpleRef;

public class HttpReaders {

	private static final byte[] END_OF_LINE = {'\r', '\n'};

	public static final int
		MAX_REQUEST_LINE_LENGTH = 65535;

	public static Reader<HttpClientHeader> clientHeaderReader() {
		Reader<HttpClientHeader> reader = new ReaderImpl<>();

		Ref<String[]> requestLine = readLine(reader, MAX_REQUEST_LINE_LENGTH).map(l -> l.split(" "));
		reader.expect(instance -> requestLine.get(instance).length == 3).orElseThrow(() -> new IOException("Invalid request line"));

		Ref<HttpRequestMethod> requestMethod = requestLine.map(l -> HttpRequestMethod.get(l[0]));
		reader.expect(instance -> requestMethod.get(instance) != null).orElseThrow(() -> new IOException("Invalid request method"));

		Ref<HttpUrlPath> path = requestLine.map(l -> HttpUrlPath.parse(l[1]));
		reader.expect(instance -> path.get(instance) != null).orElseThrow(() -> new IOException("Invalid request path"));

		Ref<String> protocolVersion = requestLine.map(l -> l[2]);

		Ref<HttpHeaderFields> fields = reader.read(headerFieldsReader());

		SimpleRef<Integer> contentLength = SimpleRef.create();

		reader.expect(instance -> {
			HttpHeaderFields f = fields.get(instance);
			String cL = f.getFirst("Content-Length");
			if(cL == null) return true;
			try {
				contentLength.set(instance, Integer.parseUnsignedInt(cL));
				return true;
			}catch(NumberFormatException e) {
				return false;
			}
		});

		SimpleRef<byte[]> postData = SimpleRef.create();
		reader.branch(instance -> contentLength.isSet(instance), Operations.lazy(instance -> Operations.readNBytes(postData, contentLength.get(instance))), null);

		// TODO: chunked

		reader.setConverter(instance -> new HttpClientHeader(requestMethod.get(instance), path.get(instance), protocolVersion.get(instance), fields.get(instance), postData.getOrElse(instance, null)));

		return reader;
	}

	public static Reader<HttpHeaderFields> headerFieldsReader() {
		Reader<HttpHeaderFields> reader = new ReaderImpl<>();

		SimpleRef<List<String>> lines = SimpleRef.createRewritable();
		SimpleRef<String> line = SimpleRef.createRewritable();
		reader.run(instance -> lines.set(instance, new ArrayList<>()));
		reader.loopUntil(instance -> line.get(instance).isEmpty(), readLine(line, MAX_REQUEST_LINE_LENGTH).thenRun(instance -> lines.get(instance).add(line.get(instance))));

		reader.setConverter(instance -> {
			List<String> rawLines = lines.get(instance);

			HttpHeaderFields fields = new HttpHeaderFields();
			for(String rawLine : rawLines) {
				if(rawLine.isEmpty()) break;
				String[] kv = rawLine.split(": ", 2);
				if(kv.length != 2) throw new IOException("Invalid header field");
				fields.add(kv[0], kv[1]);
			}

			return fields;
		});

		return reader;
	}

	private static Ref<String> readLine(Reader<?> reader, int limit) {
		SimpleRef<String> line = SimpleRef.create();
		reader.read(readLine(line, limit));
		return line;
	}

	private static Operation readLine(SimpleRef<String> ref, int limit) {
		SimpleRef<byte[]> line = SimpleRef.create();
		return Operations.readUntilBytes(line, END_OF_LINE, limit).thenRun(instance -> {
			byte[] bytes = line.get(instance);
			ref.set(instance, new String(bytes, 0, bytes.length - 2));
		});
	}

}
