package me.mrletsplay.simplehttpserver.http.header;

import java.util.List;

public interface HttpClientContentType<T> {

	public List<String> getMimeTypes();

	public T parse(HttpHeaderFields headers, byte[] content);

}
