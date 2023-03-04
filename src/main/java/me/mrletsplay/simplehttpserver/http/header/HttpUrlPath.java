package me.mrletsplay.simplehttpserver.http.header;

import java.util.Objects;

import me.mrletsplay.mrcore.http.HttpUtils;
import me.mrletsplay.simplehttpserver.http.request.urlencoded.UrlEncoded;

public class HttpUrlPath {

	private String documentPath;
	private UrlEncoded query;

	public HttpUrlPath(String documentPath, UrlEncoded query) {
		this.documentPath = documentPath;
		this.query = query;
	}

	public HttpUrlPath(String documentPath) {
		this(documentPath, new UrlEncoded());
	}

	public void setDocumentPath(String path) {
		this.documentPath = path;
	}

	public String getDocumentPath() {
		return documentPath;
	}

	public UrlEncoded getQuery() {
		return query;
	}

	public static HttpUrlPath parse(String rawPath) {
		String[] psp = rawPath.split("\\?", 2);

		String path;
		try {
			path = HttpUtils.urlDecode(psp[0]);
		}catch(IllegalArgumentException e) {
			return null;
		}

		UrlEncoded query = new UrlEncoded();
		if(psp.length == 2) {
			query = UrlEncoded.parse(psp[1]);
			if(query == null) return null;
		}

		return new HttpUrlPath(path, query);
	}

	public static HttpUrlPath of(String path) {
		return parse(path);
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof HttpUrlPath)) return false;
		HttpUrlPath o = (HttpUrlPath) obj;
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
		return new Builder(new HttpUrlPath(documentPath));
	}

	public static class Builder implements me.mrletsplay.mrcore.misc.Builder<HttpUrlPath, Builder> {

		private HttpUrlPath path;

		private Builder(HttpUrlPath path) {
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
		public HttpUrlPath create() throws IllegalStateException {
			return path;
		}

	}

}
