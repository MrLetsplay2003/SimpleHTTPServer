package me.mrletsplay.simplehttpserver.http.header;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.json.JSONParser;
import me.mrletsplay.simplehttpserver.http.request.multipart.Multipart;
import me.mrletsplay.simplehttpserver.http.request.urlencoded.URLEncoded;

public class DefaultClientContentTypes<T> implements HttpClientContentType<T> {

	private static final List<DefaultClientContentTypes<?>> DEFAULT_TYPES;

	public static final DefaultClientContentTypes<Object> JSON = new DefaultClientContentTypes<>((h, c) -> JSONParser.parse(new String(c, StandardCharsets.UTF_8)), "application/json");
	public static final DefaultClientContentTypes<JSONObject> JSON_OBJECT = new DefaultClientContentTypes<>((h, c) -> new JSONObject(new String(c, StandardCharsets.UTF_8)), "application/json");
	public static final DefaultClientContentTypes<JSONArray> JSON_ARRAY = new DefaultClientContentTypes<>((h, c) -> new JSONArray(new String(c, StandardCharsets.UTF_8)), "application/json");
	public static final DefaultClientContentTypes<String> TEXT = new DefaultClientContentTypes<>((h, c) -> new String(c, StandardCharsets.UTF_8), "text/plain");
	public static final DefaultClientContentTypes<URLEncoded> URLENCODED = new DefaultClientContentTypes<>((h, c) -> URLEncoded.parse(c), "application/x-www-form-urlencoded");
	public static final DefaultClientContentTypes<Multipart> MULTIPART = new DefaultClientContentTypes<>((h, c) -> Multipart.parse(h, c), "multipart/form-data", "multipart/mixed");
	public static final DefaultClientContentTypes<byte[]> OCTET_STREAM = new DefaultClientContentTypes<>((h, c) -> c, "application/octet-stream");

	static {
		DEFAULT_TYPES = new ArrayList<>();
		DEFAULT_TYPES.add(JSON); // Will be prioritized when using getByMimeType
		DEFAULT_TYPES.add(JSON_OBJECT);
		DEFAULT_TYPES.add(JSON_ARRAY);
		DEFAULT_TYPES.add(TEXT);
		DEFAULT_TYPES.add(URLENCODED);
		DEFAULT_TYPES.add(MULTIPART);
		DEFAULT_TYPES.add(OCTET_STREAM);
	}

	private BiFunction<HttpHeaderFields, byte[], T> parsingFunction;
	private List<String> mimeTypes;

	private DefaultClientContentTypes(BiFunction<HttpHeaderFields, byte[], T> parsingFunction, String... mimeTypes) {
		this.parsingFunction = parsingFunction;
		this.mimeTypes = Arrays.asList(mimeTypes);
	}

	@Override
	public List<String> getMimeTypes() {
		return mimeTypes;
	}

	@Override
	public T parse(HttpHeaderFields headers, byte[] content) {
		return parsingFunction.apply(headers, content);
	}

	public static DefaultClientContentTypes<?> getByMimeType(String mimeType) {
		return DEFAULT_TYPES.stream()
			.filter(t -> t.getMimeTypes().contains(mimeType.toLowerCase()))
			.findFirst().orElse(null);
	}

}
