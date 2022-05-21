package me.mrletsplay.simplehttpserver.http.header;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpHeaderFields {

	private Map<String, List<String>> fields;

	public HttpHeaderFields() {
		this.fields = new HashMap<>();
	}

	public void set(String name, String value) {
		fields.remove(name.toLowerCase());
		fields.put(name.toLowerCase(), new ArrayList<>(Arrays.asList(value)));
	}

	public void add(String name, String value) {
		List<String> vs = fields.getOrDefault(name.toLowerCase(), new ArrayList<>());
		vs.add(value);
		fields.put(name.toLowerCase(), vs);
	}

	public boolean has(String name) {
		return fields.containsKey(name);
	}

	public List<String> getAll(String name) {
		return fields.getOrDefault(name.toLowerCase(), Collections.emptyList());
	}

	public String getFirst(String name) {
		List<String> vs = getAll(name.toLowerCase());
		return vs.isEmpty() ? null : vs.get(0);
	}

	public Map<String, String> getCookies() {
		Map<String, String> cookies = new HashMap<>();
		for(String c : getAll("Cookie")) {
			for(String co : c.split("; ")) {
				String[] kv = co.split("=");
				cookies.put(kv[0], kv[1]);
			}
		}
		return cookies;
	}

	public String getCookie(String name) {
		return getCookies().get(name);
	}

	public void setCookie(String name, String value, String... properties) {
		add("Set-Cookie", name + "=" + value + (properties.length == 0 ? "" : "; " + Arrays.stream(properties).collect(Collectors.joining("; "))));
	}

	public Map<String, List<String>> getRaw() {
		return fields;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof HttpHeaderFields)) return false;
		HttpHeaderFields o = (HttpHeaderFields) obj;
		return fields.equals(o.fields);
	}

	@Override
	public int hashCode() {
		return fields.hashCode();
	}

}
