package me.mrletsplay.simplehttpserver.http.validation.result;

import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.simplehttpserver.http.response.JsonResponse;

public class ValidationResult {

	private static final ValidationResult OK = new ValidationResult(true, null);

	private boolean ok;
	private ValidationErrors errors;

	private ValidationResult(boolean ok, ValidationErrors errors) {
		this.ok = ok;
		this.errors = errors;
	}

	public boolean isOk() {
		return ok;
	}

	public ValidationErrors getErrors() {
		return ok ? null : errors;
	}

	public static ValidationResult ok() {
		return OK;
	}

	public static ValidationResult error(ValidationErrors errors) {
		return new ValidationResult(false, errors);
	}

	public static ValidationResult error(String key, String... messages) {
		return error(ValidationErrors.of(key, messages));
	}

	public JsonResponse asJsonResponse() {
		if(ok) throw new IllegalStateException("Only usable if there is an error");
		JSONObject obj = new JSONObject();
		obj.put("errors", errors.toJson());
		return new JsonResponse(obj);
	}

}
