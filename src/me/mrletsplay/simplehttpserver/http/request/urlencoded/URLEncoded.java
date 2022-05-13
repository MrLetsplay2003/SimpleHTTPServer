package me.mrletsplay.simplehttpserver.http.request.urlencoded;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.mrletsplay.simplehttpserver.http.header.HttpURLPath;

public class URLEncoded {

	private Map<String, List<String>> data;

	public URLEncoded(Map<String, List<String>> map) {
		this.data = new LinkedHashMap<>(map);
	}

	public URLEncoded() {
		this.data = new LinkedHashMap<>();
	}

	public boolean has(String key) {
		return data.containsKey(key);
	}

	public List<String> getAll(String key) {
		return data.getOrDefault(key, Collections.emptyList());
	}

	public String getFirst(String key) {
		List<String> d = data.get(key);
		return d == null ? null : d.get(0);
	}

	public static URLEncoded parse(byte[] postData) {
		return new URLEncoded(HttpURLPath.parseQueryParameters(new String(postData, StandardCharsets.UTF_8)));
	}

}
