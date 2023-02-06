package me.mrletsplay.simplehttpserver.http.header;

import java.util.Objects;

import me.mrletsplay.simplehttpserver.http.request.urlencoded.URLEncoded;

public class HttpURLPath {

	private String documentPath;
	private URLEncoded query;

	public HttpURLPath(String documentPath, URLEncoded query) {
		this.documentPath = documentPath;
		this.query = query;
	}

	public HttpURLPath(String documentPath) {
		this(documentPath, new URLEncoded());
	}

	public void setDocumentPath(String path) {
		this.documentPath = path;
	}

	public String getDocumentPath() {
		return documentPath;
	}

	public URLEncoded getQuery() {
		return query;
	}

	public static HttpURLPath parse(String rawPath) {
		String[] psp = rawPath.split("\\?", 2);
		String path = psp[0];
		URLEncoded query = new URLEncoded();
		if(psp.length == 2) query = URLEncoded.parse(psp[1]);
		return new HttpURLPath(path, query);
	}

	public static HttpURLPath of(String path) {
		return parse(path);
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof HttpURLPath)) return false;
		HttpURLPath o = (HttpURLPath) obj;
		return documentPath.equals(o.documentPath)
				&& query.equals(o.query);
	}

	@Override
	public int hashCode() {
		return Objects.hash(documentPath, query);
	}

	@Override
	public String toString() {
		return documentPath + (query.isEmpty() ? "" : "?" + query.toString());
	}

	public static Builder builder(String documentPath) {
		return new Builder(new HttpURLPath(documentPath));
	}

	public static class Builder implements me.mrletsplay.mrcore.misc.Builder<HttpURLPath, Builder> {

		private HttpURLPath path;

		private Builder(HttpURLPath path) {
			this.path = path;
		}

		public Builder addQuery(String key, String value) {
			path.getQuery().add(key, value);
			return this;
		}

		public Builder setQuery(String key, String value) {
			path.getQuery().set(key, value);
			return this;
		}

		@Override
		public HttpURLPath create() throws IllegalStateException {
			return path;
		}

	}

}
