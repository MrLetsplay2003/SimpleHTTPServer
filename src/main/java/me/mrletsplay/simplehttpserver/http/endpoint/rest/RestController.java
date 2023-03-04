package me.mrletsplay.simplehttpserver.http.endpoint.rest;

import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;
import me.mrletsplay.simplehttpserver.http.endpoint.Endpoint;
import me.mrletsplay.simplehttpserver.http.endpoint.EndpointCollection;
import me.mrletsplay.simplehttpserver.http.endpoint.RequestParameter;

public abstract class RestController implements EndpointCollection {

	private String basePath;

	public RestController(String basePath) {
		this.basePath = basePath;
	}

	@Endpoint(path = "")
	public abstract void index();

	@Endpoint(method = HttpRequestMethod.POST, path = "")
	public abstract void store();

	@Endpoint(path = "/{id}", pathPattern = true)
	public abstract void show(@RequestParameter("id") String id);

	@Endpoint(method = HttpRequestMethod.PUT, path = "/{id}", pathPattern = true)
	public abstract void update(@RequestParameter("id") String id);

	@Endpoint(method = HttpRequestMethod.PATCH, path = "/{id}", pathPattern = true)
	public abstract void updatePartial(@RequestParameter("id") String id);

	@Endpoint(method = HttpRequestMethod.DELETE, path = "/{id}", pathPattern = true)
	public abstract void destroy(@RequestParameter("id") String id);

	@Override
	public String getBasePath() {
		return basePath;
	}

}
