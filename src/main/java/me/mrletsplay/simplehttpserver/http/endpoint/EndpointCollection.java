package me.mrletsplay.simplehttpserver.http.endpoint;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;
import me.mrletsplay.simplehttpserver.http.HttpStatusCodes;
import me.mrletsplay.simplehttpserver.http.document.DocumentProvider;
import me.mrletsplay.simplehttpserver.http.document.HttpDocument;
import me.mrletsplay.simplehttpserver.http.endpoint.exception.EndpointException;
import me.mrletsplay.simplehttpserver.http.exception.HttpResponseException;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.util.PathMatcher;

public interface EndpointCollection extends HttpDocument {

	public default String getBasePath() {
		return "";
	}

	public default Collection<EndpointDescriptor> getEndpoints() {
		List<EndpointDescriptor> endpoints = new ArrayList<>();

		for(Method m : getClass().getMethods()) {
			Endpoint endpoint = EndpointUtil.getEndpoint(m);
			if(endpoint == null) continue;
			endpoints.add(new EndpointDescriptor(endpoint.method(), getBasePath() + endpoint.path(), endpoint.pathPattern()));
		}

		return endpoints;
	}

	@Override
	public default void createContent() throws HttpResponseException {
		HttpRequestContext ctx = HttpRequestContext.getCurrentContext();
		HttpRequestMethod method = ctx.getClientHeader().getMethod();
		String path = ctx.getRequestedPath().getDocumentPath();

		for(Method m : getClass().getMethods()) {
			Endpoint endpoint = EndpointUtil.getEndpoint(m);
			if(endpoint == null) continue;

			if(method == endpoint.method()) {
				String endpointPath = getBasePath() + endpoint.path();

				Map<String, String> params = null;
				if(endpoint.pathPattern()) {
					params = PathMatcher.match(endpointPath, path);
					if(params == null) continue;
				}else {
					if(!path.equals(endpointPath)) continue;
				}

				if(!Arrays.stream(m.getParameterTypes()).allMatch(t -> t.equals(String.class))) {
					throw new EndpointException("Invalid endpoint definition: Endpoints may only have String parameters");
				}

				RequestParameter[] requestParams = EndpointUtil.getRequestParameters(m);
				if(requestParams == null) {
					throw new EndpointException("Invalid endpoint definition: Endpoint parameters must be annotated with @RequestParameter");
				}

				if(requestParams.length > 0 && params == null) {
					throw new EndpointException("Invalid endpoint definition: Endpoint has parameters but no path pattern");
				}

				Object[] args = new Object[requestParams.length];
				for(int i = 0; i < requestParams.length; i++) {
					@SuppressWarnings("null")
					String value = params.get(requestParams[i].value());
					if(value == null) {
						throw new EndpointException("Invalid endpoint definition: Endpoint parameter doesn't exist in path pattern");
					}

					args[i] = value;
				}

				try {
					m.setAccessible(true);
					m.invoke(this, args);
					return;
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new EndpointException(e);
				}
			}
		}

		throw new HttpResponseException(HttpStatusCodes.INTERNAL_SERVER_ERROR_500, "Requested endpoint doesn't exist");
	}

	public default void register(DocumentProvider provider) {
		getEndpoints().forEach(descriptor -> {
			if(descriptor.isPattern()) {
				provider.registerPattern(descriptor.getMethod(), descriptor.getPath(), this);
			}else {
				provider.register(descriptor.getMethod(), descriptor.getPath(), this);
			}
		});
	}

	public default void unregister(DocumentProvider provider) {
		getEndpoints().forEach(descriptor -> {
			if(descriptor.isPattern()) {
				provider.unregisterPattern(descriptor.getMethod(), descriptor.getPath());
			}else {
				provider.unregister(descriptor.getMethod(), descriptor.getPath());
			}
		});
	}

}
