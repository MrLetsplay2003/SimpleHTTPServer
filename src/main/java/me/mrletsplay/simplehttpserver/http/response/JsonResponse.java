package me.mrletsplay.simplehttpserver.http.response;

import java.nio.charset.StandardCharsets;

import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.simplehttpserver.http.util.MimeType;

public class JsonResponse extends FixedHttpResponse {

	public static final JsonResponse
		EMPTY_OBJECT = new JsonResponse(new JSONObject()),
		EMPTY_ARRAY = new JsonResponse(new JSONArray());

	private String json;

	public JsonResponse(JSONObject object) {
		this.json = object.toString();
	}

	public JsonResponse(JSONArray array) {
		this.json = array.toString();
	}

	@Override
	public byte[] getContentBytes() {
		return json.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	public MimeType getContentType() {
		return MimeType.JSON;
	}

}
