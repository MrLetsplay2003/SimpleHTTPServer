package me.mrletsplay.simplehttpserver.http.document;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;

public class DocumentMap {

	private Map<String, DocumentMapEntry> documents;

	public DocumentMap() {
		this.documents = new HashMap<>();
	}

	public void put(HttpRequestMethod method, String path, HttpDocument document) {
		DocumentMapEntry entry = documents.computeIfAbsent(path, key -> new DocumentMapEntry());
		entry.put(method, document);
	}

	public void remove(HttpRequestMethod method, String path) {
		DocumentMapEntry entry = documents.get(path);
		if(entry == null) return;

		entry.remove(method);

		if(entry.isEmpty()) documents.remove(path);
	}

	public HttpDocument get(HttpRequestMethod method, String path) {
		DocumentMapEntry entry = documents.get(path);
		if(entry == null) return null;

		return entry.get(method);
	}

	public Set<String> getPaths() {
		return Collections.unmodifiableSet(documents.keySet());
	}

	public Set<HttpRequestMethod> getOptions(String path) {
		return Optional.ofNullable(documents.get(path))
			.map(p -> p.getOptions())
			.orElse(Collections.emptySet());
	}

	private static class DocumentMapEntry {

		private Map<HttpRequestMethod, HttpDocument> map;

		public DocumentMapEntry() {
			this.map = new HashMap<>();
		}

		public void put(HttpRequestMethod method, HttpDocument document) {
			map.put(method, document);
		}

		public void remove(HttpRequestMethod method) {
			map.remove(method);
		}

		public HttpDocument get(HttpRequestMethod method) {
			return map.get(method);
		}

		public Set<HttpRequestMethod> getOptions() {
			return Collections.unmodifiableSet(map.keySet());
		}

		public boolean isEmpty() {
			return map.isEmpty();
		}

	}

}
