package me.mrletsplay.simplehttpserver.http.request.urlencoded;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import me.mrletsplay.mrcore.http.HttpUtils;

public class URLEncoded {

	private Map<String, List<String>> data;

	public URLEncoded(Map<String, List<String>> map) {
		this.data = new LinkedHashMap<>(map);
	}

	public URLEncoded() {
		this.data = new LinkedHashMap<>();
	}

	public void set(String name, String value) {
		data.remove(name.toLowerCase());
		data.put(name.toLowerCase(), new ArrayList<>(Arrays.asList(value)));
	}

	public void add(String name, String value) {
		List<String> vs = data.getOrDefault(name.toLowerCase(), new ArrayList<>());
		vs.add(value);
		data.put(name.toLowerCase(), vs);
	}

	public boolean isEmpty() {
		return data.isEmpty();
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

	public String getFirst(String key, String fallback) {
		List<String> d = data.get(key);
		return d == null ? null : d.get(0);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		URLEncoded other = (URLEncoded) obj;
		if (data == null) {
			if (other.data != null)
				return false;
		} else if (!data.equals(other.data))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return data.entrySet().stream()
			.map(en -> en.getValue().stream()
				.map(v -> HttpUtils.urlEncode(en.getKey()) + "=" + HttpUtils.urlEncode(v))
				.collect(Collectors.joining("&")))
			.collect(Collectors.joining("&"));
	}

	public static URLEncoded parse(String urlEncoded) {
		Map<String, List<String>> queryParameters = new HashMap<>();
		for(String st : urlEncoded.split("&")) {
			String[] kv = st.split("=", 2);
			String key = HttpUtils.urlDecode(kv[0]);
			List<String> vs = queryParameters.getOrDefault(key, new ArrayList<>());
			vs.add(kv.length == 2 ? HttpUtils.urlDecode(kv[1]) : "");
			queryParameters.put(key, vs);
		}
		return new URLEncoded(queryParameters);
	}

	public static URLEncoded parse(byte[] urlEncoded) {
		return parse(new String(urlEncoded, StandardCharsets.UTF_8));
	}

}
