package me.mrletsplay.simplehttpserver.http.document;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.util.PathMatcher;

public class DefaultDocumentProvider implements DocumentProvider {

	private DocumentMap documents;
	private DocumentMap patternDocuments;

	private HttpDocument
		document404,
		document500;

	public DefaultDocumentProvider() {
		this.documents = new DocumentMap();
		this.patternDocuments = new DocumentMap();
		setNotFoundDocument(new DefaultNotFoundDocument());
		setErrorDocument(new DefaultErrorDocument());
	}

	@Override
	public void register(HttpRequestMethod method, String path, HttpDocument document) {
		documents.put(method, path, document);
	}

	@Override
	public void unregister(HttpRequestMethod method, String path) {
		documents.remove(method, path);
	}

	@Override
	public void registerPattern(HttpRequestMethod method, String pattern, HttpDocument document) {
		patternDocuments.put(method, pattern, document);
	}

	@Override
	public void unregisterPattern(HttpRequestMethod method, String pattern) {
		patternDocuments.remove(method, pattern);
	}

	@Override
	public HttpDocument get(HttpRequestMethod method, String path) {
		HttpDocument d = documents.get(method, path);
		if(d != null) return d;

		for(var mapPath : patternDocuments.getPaths()) {
			Map<String, String> params = PathMatcher.match(mapPath, path);
			if(params != null) {
				HttpRequestContext ctx = HttpRequestContext.getCurrentContext();
				if(ctx != null) ctx.setPathParameters(params);
				return patternDocuments.get(method, mapPath);
			}
		}

		return null;
	}

	@Override
	public Set<HttpRequestMethod> getOptions(String path) {
		Set<HttpRequestMethod> options = documents.getOptions(path);
		if(!options.isEmpty()) return options;

		for(var mapPath : patternDocuments.getPaths()) {
			Map<String, String> params = PathMatcher.match(mapPath, path);
			if(params != null) {
				try {
					HttpRequestContext ctx = HttpRequestContext.getCurrentContext();
					ctx.setPathParameters(params);
				}catch(IllegalStateException e) {}
				return patternDocuments.getOptions(mapPath);
			}
		}

		return Collections.emptySet();
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
