package me.mrletsplay.simplehttpserver.http.validation.result;

import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.simplehttpserver.http.response.JsonResponse;

public class ValidationResult {

	private static final ValidationResult OK = new ValidationResult(true, null);

	private final boolean ok;
	private final ValidationErrors errors;

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

	public ValidationResult combine(ValidationResult other) {
		if(isOk() && other.isOk()) return OK;
		return error(ValidationErrors.combine(errors, other.errors));
	}

	public ValidationResult asSubElement(String key) {
		if(isOk()) return this;
		return error(ValidationErrors.subElement(key, errors));
	}

	public static ValidationResult error(ValidationErrors errors) {
		return new ValidationResult(false, errors);
	}

	public static ValidationResult error(String key, String... messages) {
		return error(ValidationErrors.of(key, messages));
	}

	public static ValidationResult check(boolean bool, String key, String errorMessage) {
		return bool ? ValidationResult.ok() : ValidationResult.error(key, errorMessage);
	}

	public JsonResponse asJsonResponse() {
		if(ok) throw new IllegalStateException("Only usable if there is an error");
		JSONObject obj = new JSONObject();
		obj.put("errors", errors.toJson());
		return new JsonResponse(obj);
	}

}
