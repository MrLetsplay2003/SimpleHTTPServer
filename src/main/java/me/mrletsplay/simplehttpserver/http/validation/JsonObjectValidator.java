package me.mrletsplay.simplehttpserver.http.validation;

import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.json.JSONType;
import me.mrletsplay.simplehttpserver.http.validation.result.ValidationErrors;
import me.mrletsplay.simplehttpserver.http.validation.result.ValidationResult;

public class JsonObjectValidator extends AbstractValidator<JSONObject> {

	private static ValidationRule<JSONObject> ruleRequire(String key) {
		return o -> ValidationResult.check(o.has(key), key, "Missing attribute");
	}

	private static ValidationRule<JSONObject> ruleNonNull(String key) {
		return o -> ValidationResult.check(!o.has(key) || o.get(key) != null, key, "Attribute must not be null");
	}

	private static ValidationRule<JSONObject> ruleIsOfType(String key, JSONType type) {
		return o -> {
			if(!o.has(key)) return ValidationResult.ok();
			return ValidationResult.check(o.isOfType(key, type), key, "Attribute must be of type " + type);
		};
	}

	private static ValidationRule<JSONObject> ruleEmail(String key) {
		return ruleIsOfType(key, JSONType.STRING).bailAnd(o -> {
			if(!o.has(key)) return ValidationResult.ok();
			String email = o.getString(key);
			if(email == null) return ValidationResult.ok();
			return ValidationResult.check(ValidationUtil.isEmail(email), key, "Not an email address");
		});
	}

	private static ValidationRule<JSONObject> ruleSubElementMatches(String key, JsonObjectValidator validator) {
		return ruleIsOfType(key, JSONType.OBJECT).bailAnd(o -> {
			if(!o.has(key)) return ValidationResult.ok();
			JSONObject object = o.getJSONObject(key);
			if(object == null) return ValidationResult.ok();
			ValidationResult result = validator.validate(object);
			if(result.isOk()) return result;
			return ValidationResult.error(ValidationErrors.subElement(key, result.getErrors()));
		});
	}

	private static ValidationRule<JSONObject> ruleSubElementMatches(String key, JsonArrayValidator validator) {
		return ruleIsOfType(key, JSONType.ARRAY).bailAnd(o -> {
			if(!o.has(key)) return ValidationResult.ok();
			JSONArray array = o.getJSONArray(key);
			if(array == null) return ValidationResult.ok();
			ValidationResult result = validator.validate(array);
			if(result.isOk()) return result;
			return ValidationResult.error(ValidationErrors.subElement(key, result.getErrors()));
		});
	}

	public JsonObjectValidator require(String key) {
		addRule(ruleRequire(key));
		return this;
	}

	public JsonObjectValidator requireNonNull(String key) {
		addRule(ruleRequire(key)
			.bailAnd(ruleNonNull(key)));
		return this;
	}

	public JsonObjectValidator require(String key, JSONType type) {
		addRule(ruleRequire(key)
			.bailAnd(ruleIsOfType(key, type)));
		return this;
	}

	public JsonObjectValidator requireNonNull(String key, JSONType type) {
		addRule(ruleRequire(key)
			.bailAnd(ruleNonNull(key))
			.bailAnd(ruleIsOfType(key, type)));
		return this;
	}

	public JsonObjectValidator optional(String key, JSONType type) {
		addRule(ruleIsOfType(key, type));
		return this;
	}

	public JsonObjectValidator optionalNonNull(String key, JSONType type) {
		addRule(ruleNonNull(key)
			.bailAnd(ruleIsOfType(key, type)));
		return this;
	}

	public JsonObjectValidator requireEmail(String key) {
		addRule(ruleRequire(key)
			.bailAnd(ruleEmail(key)));
		return this;
	}

	public JsonObjectValidator optionalEmail(String key) {
		addRule(ruleEmail(key));
		return this;
	}

	public JsonObjectValidator requireEmailNonNull(String key) {
		addRule(ruleRequire(key)
			.bailAnd(ruleNonNull(key))
			.bailAnd(ruleEmail(key)));
		return this;
	}

	public JsonObjectValidator optionalEmailNonNull(String key) {
		addRule(ruleNonNull(key)
			.bailAnd(ruleEmail(key)));
		return this;
	}

	public JsonObjectValidator requireObject(String key, JsonObjectValidator validator) {
		addRule(ruleRequire(key)
			.bailAnd(ruleSubElementMatches(key, validator)));
		return this;
	}

	public JsonObjectValidator optionalObject(String key, JsonObjectValidator validator) {
		addRule(ruleSubElementMatches(key, validator));
		return this;
	}

	public JsonObjectValidator requireObjectNonNull(String key, JsonObjectValidator validator) {
		addRule(ruleRequire(key)
			.bailAnd(ruleNonNull(key))
			.bailAnd(ruleSubElementMatches(key, validator)));
		return this;
	}

	public JsonObjectValidator optionalObjectNonNull(String key, JsonObjectValidator validator) {
		addRule(ruleNonNull(key)
			.bailAnd(ruleSubElementMatches(key, validator)));
		return this;
	}

	public JsonObjectValidator requireArray(String key, JsonArrayValidator validator) {
		addRule(ruleRequire(key)
			.bailAnd(ruleSubElementMatches(key, validator)));
		return this;
	}

	public JsonObjectValidator optionalArray(String key, JsonArrayValidator validator) {
		addRule(ruleSubElementMatches(key, validator));
		return this;
	}

	public JsonObjectValidator requireArrayNonNull(String key, JsonArrayValidator validator) {
		addRule(ruleRequire(key)
			.bailAnd(ruleNonNull(key))
			.bailAnd(ruleSubElementMatches(key, validator)));
		return this;
	}

	public JsonObjectValidator optionalArrayNonNull(String key, JsonArrayValidator validator) {
		addRule(ruleNonNull(key)
			.bailAnd(ruleSubElementMatches(key, validator)));
		return this;
	}

}
