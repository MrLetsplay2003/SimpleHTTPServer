package me.mrletsplay.simplehttpserver.http.endpoint.validation;

import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.json.JSONType;

public class JsonObjectValidator extends AbstractValidator<JSONObject> {

	public JsonObjectValidator require(String key) {
		addRule(o -> o.has(key) ? ValidationResult.ok() : ValidationResult.error("Missing the '" + key + "' attribute"));
		return this;
	}

	public JsonObjectValidator require(String key, JSONType type) {
		addRule(o -> o.isOfType(key, type) ? ValidationResult.ok() : ValidationResult.error("Missing the '" + key + "' attribute of type " + type));
		return this;
	}

	public JsonObjectValidator optional(String key, JSONType type) {
		addRule(o -> {
			if(!o.has(key) || o.isOfType(key, type)) return ValidationResult.ok();
			return ValidationResult.error("Attribute '" + key + "' is not of type " + type);
		});
		return this;
	}

}
