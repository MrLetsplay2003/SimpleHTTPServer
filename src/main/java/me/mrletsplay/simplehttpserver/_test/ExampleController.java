package me.mrletsplay.simplehttpserver._test;

import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.json.JSONType;
import me.mrletsplay.simplehttpserver.http.HttpStatusCodes;
import me.mrletsplay.simplehttpserver.http.endpoint.rest.PartialRestController;
import me.mrletsplay.simplehttpserver.http.header.DefaultClientContentTypes;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.request.multipart.Multipart;
import me.mrletsplay.simplehttpserver.http.request.multipart.MultipartBodyPart;
import me.mrletsplay.simplehttpserver.http.response.JsonResponse;
import me.mrletsplay.simplehttpserver.http.response.TextResponse;
import me.mrletsplay.simplehttpserver.http.validation.JsonObjectValidator;
import me.mrletsplay.simplehttpserver.http.validation.result.ValidationResult;

public class ExampleController extends PartialRestController {

	private static final JsonObjectValidator VALIDATOR = new JsonObjectValidator()
		.require("id", JSONType.STRING)
		.require("additional_data")
		.optional("test", JSONType.BOOLEAN)
		.requireObject("user", new JsonObjectValidator()
			.require("name", JSONType.STRING)
			.requireEmail("email")
			.optionalEmail("secondary_email")
			.optional("nickname", JSONType.STRING))
		.optionalObject("user_info", new JsonObjectValidator()
			.require("address", JSONType.STRING)
			.optional("tag", JSONType.INTEGER));

	public ExampleController() {
		super("/example");
	}

	@Override
	public void index() {
		HttpRequestContext.getCurrentContext().respond(HttpStatusCodes.OK_200, new JsonResponse(new JSONArray()));
	}

	@Override
	public void store() {
		HttpRequestContext ctx = HttpRequestContext.getCurrentContext();
		Multipart object;
		if((object = ctx.expectContent(DefaultClientContentTypes.MULTIPART)) == null) {
			ctx.respond(HttpStatusCodes.BAD_REQUEST_400, new TextResponse("Invalid content type"));
			return;
		}

		for(MultipartBodyPart bp : object.getBodyParts()) {
			System.out.println("BP: " + bp.getHeaders().getRaw());
			System.out.println(new String(bp.getData()));
		}
		ctx.respond(HttpStatusCodes.CREATED_201, new JsonResponse(new JSONObject()));
	}

	@Override
	public void update(String id) {
		HttpRequestContext ctx = HttpRequestContext.getCurrentContext();
		JSONObject object;
		if((object = ctx.expectContent(DefaultClientContentTypes.JSON_OBJECT)) == null) {
			ctx.respond(HttpStatusCodes.BAD_REQUEST_400, new TextResponse("Invalid content type"));
			return;
		}

		ValidationResult result = VALIDATOR.validate(object);
		if(!result.isOk()) {
			ctx.respond(HttpStatusCodes.BAD_REQUEST_400, result.asJsonResponse());
			return;
		}

		ctx.respond(HttpStatusCodes.OK_200, new JsonResponse(new JSONObject()));
	}

	@Override
	public void show(String id) {
		HttpRequestContext.getCurrentContext().respond(HttpStatusCodes.OK_200, new JsonResponse(new JSONObject()));
	}

}
