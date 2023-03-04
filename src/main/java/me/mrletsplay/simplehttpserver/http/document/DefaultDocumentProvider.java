package me.mrletsplay.simplehttpserver.http.document;

import java.util.HashMap;
import java.util.Map;

import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.util.PathMatcher;

public class DefaultDocumentProvider implements DocumentProvider {

	private Map<MethodAndPath, HttpDocument> documents;
	private Map<MethodAndPath, HttpDocument> patternDocuments;

	private HttpDocument
		document404,
		document500;

	public DefaultDocumentProvider() {
		this.documents = new HashMap<>();
		this.patternDocuments = new HashMap<>();
		setNotFoundDocument(new DefaultNotFoundDocument());
		setErrorDocument(new DefaultErrorDocument());
	}

	@Override
	public void register(HttpRequestMethod method, String path, HttpDocument document) {
		documents.put(new MethodAndPath(method, path), document);
	}

	@Override
	public void unregister(HttpRequestMethod method, String path) {
		documents.remove(new MethodAndPath(method, path));
	}

	@Override
	public void registerPattern(HttpRequestMethod method, String pattern, HttpDocument document) {
		patternDocuments.put(new MethodAndPath(method, pattern), document);
	}

	@Override
	public void unregisterPattern(HttpRequestMethod method, String pattern) {
		patternDocuments.remove(new MethodAndPath(method, pattern));
	}

	@Override
	public HttpDocument get(HttpRequestMethod method, String path) {
		HttpDocument d = documents.get(new MethodAndPath(method, path));
		if(d != null) return d;

		for(var kv : patternDocuments.entrySet()) {
			if(kv.getKey().getMethod() != method) continue;

			Map<String, String> params = PathMatcher.match(kv.getKey().getPath(), path);
			if(params != null) {
				HttpRequestContext ctx = HttpRequestContext.getCurrentContext();
				if(ctx != null) ctx.setPathParameters(params);
				return kv.getValue();
			}
		}

		return null;
	}

	@Override
	public void setNotFoundDocument(HttpDocument document) {
		this.document404 = document;
	}

	@Override
	public HttpDocument getNotFoundDocument() {
		return document404;
	}

	@Override
	public void setErrorDocument(HttpDocument document) {
		this.document500 = document;
	}

	@Override
	public HttpDocument getErrorDocument() {
		return document500;
	}

}
