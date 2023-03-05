package me.mrletsplay.simplehttpserver.http.endpoint.rest;

import me.mrletsplay.simplehttpserver.http.endpoint.UnavailableEndpoint;

public class PartialRestController extends RestController {

	public PartialRestController(String basePath) {
		super(basePath);
	}

	@UnavailableEndpoint
	@Override
	public void index() {}

	@UnavailableEndpoint
	@Override
	public void store() {}

	@UnavailableEndpoint
	@Override
	public void show(String id) {}

	@UnavailableEndpoint
	@Override
	public void update(String id) {}

	@UnavailableEndpoint
	@Override
	public void updatePartial(String id) {}

	@UnavailableEndpoint
	@Override
	public void destroy(String id) {}

}
