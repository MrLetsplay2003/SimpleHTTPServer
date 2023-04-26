package me.mrletsplay.simplehttpserver.http.endpoint.validation;

import java.util.Collections;
import java.util.List;

import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.simplehttpserver.http.response.JsonResponse;

public class ValidationResult {

	private static final ValidationResult OK = new ValidationResult(true, null);

	private boolean ok;
	private List<String> errors;

	private ValidationResult(boolean ok, List<String> errors) {
		this.ok = ok;
		this.errors = errors;
	}

	public boolean isOk() {
		return ok;
	}

	public List<String> getErrors() {
		return ok ? Collections.emptyList() : errors;
	}

	public static ValidationResult ok() {
		return OK;
	}

	public static ValidationResult error(String... errors) {
		if(errors.length == 0) throw new IllegalArgumentException("Must provide at least one error");
		return new ValidationResult(false, List.of(errors));
	}

	public static ValidationResult error(List<String> errors) {
		if(errors.isEmpty()) throw new IllegalArgumentException("Must provide at least one error");
		return new ValidationResult(false, List.copyOf(errors));
	}

	public JsonResponse asJsonResponse() {
		if(ok) throw new IllegalStateException("Only usable if there is an error");
		JSONObject obj = new JSONObject();
		JSONArray errors = new JSONArray();
		errors.addAll(this.errors);
		obj.put("errors", errors);
		return new JsonResponse(obj);
	}

}
