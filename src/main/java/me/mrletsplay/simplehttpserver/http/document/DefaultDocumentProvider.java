package me.mrletsplay.simplehttpserver.http.document;

import java.util.HashMap;
import java.util.Map;

import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.util.PathMatcher;

public class DefaultDocumentProvider implements HttpDocumentProvider {

	private Map<String, HttpDocument> documents;
	private Map<String, HttpDocument> patternDocuments;

	private HttpDocument
		document404,
		document500;

	public DefaultDocumentProvider() {
		this.documents = new HashMap<>();
		this.patternDocuments = new HashMap<>();
		set404Document(new Default404Document());
		set500Document(new Default500Document());
	}

	@Override
	public void registerDocument(String path, HttpDocument document) {
		documents.put(path, document);
	}

	@Override
	public void unregisterDocument(String path) {
		documents.remove(path);
	}

	@Override
	public void registerDocumentPattern(String pattern, HttpDocument document) {
		patternDocuments.put(pattern, document);
	}

	@Override
	public void unregisterDocumentPattern(String pattern) {
		patternDocuments.remove(pattern);
	}

	@Override
	public HttpDocument getDocument(String path) {
		HttpDocument d = documents.get(path);
		if(d != null) return d;

		for(var kv : patternDocuments.entrySet()) {
			Map<String, String> params = PathMatcher.match(kv.getKey(), path);
			if(params != null) {
				HttpRequestContext ctx = HttpRequestContext.getCurrentContext();
				if(ctx != null) ctx.setPathParameters(params);
				return kv.getValue();
			}
		}

		return null;
	}

	@Override
	public void set404Document(HttpDocument document) {
		this.document404 = document;
	}

	@Override
	public HttpDocument get404Document() {
		return document404;
	}

	@Override
	public void set500Document(HttpDocument document) {
		this.document500 = document;
	}

	@Override
	public HttpDocument get500Document() {
		return document500;
	}

}
