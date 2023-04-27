package me.mrletsplay.simplehttpserver.http.validation;

import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.json.JSONType;
import me.mrletsplay.simplehttpserver.http.validation.result.ValidationErrors;
import me.mrletsplay.simplehttpserver.http.validation.result.ValidationResult;

public class JsonObjectValidator extends AbstractValidator<JSONObject> {

	public JsonObjectValidator require(String key) {
		addRule(o -> o.has(key) ? ValidationResult.ok() : ValidationResult.error(key, "Missing attribute"));
		return this;
	}

	public JsonObjectValidator require(String key, JSONType type) {
		addRule(o -> o.isOfType(key, type) ? ValidationResult.ok() : ValidationResult.error(key, "Missing attribute of type " + type));
		return this;
	}

	public JsonObjectValidator optional(String key, JSONType type) {
		addRule(o -> {
			if(!o.has(key) || o.isOfType(key, type)) return ValidationResult.ok();
			return ValidationResult.error(key, "Attribute is not of type " + type);
		});
		return this;
	}

	public JsonObjectValidator requireObject(String key, JsonObjectValidator validator) {
		addRule(o -> {
			if(!o.isOfType(key, JSONType.OBJECT)) return ValidationResult.error(key, "Missing object");
			ValidationResult result = validator.validate(o.getJSONObject(key));
			if(result.isOk()) return result;
			return ValidationResult.error(ValidationErrors.subElement(key, result.getErrors()));
		});
		return this;
	}

	public JsonObjectValidator optionalObject(String key, JsonObjectValidator validator) {
		addRule(o -> {
			if(!o.has(key)) return ValidationResult.ok();
			if(!o.isOfType(key, JSONType.OBJECT)) return ValidationResult.error(key, "Attribute is not an object");
			ValidationResult result = validator.validate(o.getJSONObject(key));
			if(result.isOk()) return result;
			return ValidationResult.error(ValidationErrors.subElement(key, result.getErrors()));
		});
		return this;
	}

}
