package me.mrletsplay.simplehttpserver.http.validation;

import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.json.JSONType;
import me.mrletsplay.simplehttpserver.http.validation.result.ValidationErrors;
import me.mrletsplay.simplehttpserver.http.validation.result.ValidationResult;

public class JsonArrayValidator extends AbstractValidator<JSONArray> {

	private static ValidationRule<JSONArray> ruleRequire(int index) {
		return a -> ValidationResult.check(a.has(index), String.valueOf(index), "Missing element");
	}

	private static ValidationRule<JSONArray> ruleNonNull(int index) {
		return a -> ValidationResult.check(!a.has(index) || a.get(index) != null, String.valueOf(index), "Element must not be null");
	}

	private static ValidationRule<JSONArray> ruleIsOfType(int index, JSONType type) {
		return a -> {
			if(!a.has(index)) return ValidationResult.ok();
			return ValidationResult.check(a.isOfType(index, type), String.valueOf(index), "Element must be of type " + type);
		};
	}

	private static ValidationRule<JSONArray> ruleAllNonNull() {
		return a -> {
			ValidationResult result = ValidationResult.ok();
			for(int i = 0; i < a.size(); i++) {
				result = result.combine(ValidationResult.check(a.get(i) != null, String.valueOf(i), "Element must not be null"));
			}
			return result;
		};
	}

	private static ValidationRule<JSONArray> ruleAllOfType(JSONType type) {
		return a -> {
			ValidationResult result = ValidationResult.ok();
			for(int i = 0; i < a.size(); i++) {
				result = result.combine(ValidationResult.check(a.isOfType(i, type), String.valueOf(i), "Element must be of type " + type));
			}
			return result;
		};
	}

	private static ValidationRule<JSONArray> ruleEmail(int index) {
		return ruleIsOfType(index, JSONType.STRING).bailAnd(a -> {
			if(!a.has(index)) return ValidationResult.ok();
			String email = a.getString(index);
			if(email == null) return ValidationResult.ok();
			return ValidationResult.check(ValidationUtil.isEmail(email), String.valueOf(index), "Not an email address");
		});
	}

	private static ValidationRule<JSONArray> ruleSubElementMatches(int index, JsonObjectValidator validator) {
		return ruleIsOfType(index, JSONType.OBJECT).bailAnd(a -> {
			if(!a.has(index)) return ValidationResult.ok();
			JSONObject object = a.getJSONObject(index);
			if(object == null) return ValidationResult.ok();
			ValidationResult result = validator.validate(object);
			if(result.isOk()) return result;
			return ValidationResult.error(ValidationErrors.subElement(String.valueOf(index), result.getErrors()));
		});
	}

	private static ValidationRule<JSONArray> ruleSubElementMatches(int index, JsonArrayValidator validator) {
		return ruleIsOfType(index, JSONType.ARRAY).bailAnd(a -> {
			if(!a.has(index)) return ValidationResult.ok();
			JSONArray array = a.getJSONArray(index);
			if(array == null) return ValidationResult.ok();
			ValidationResult result = validator.validate(array);
			if(result.isOk()) return result;
			return ValidationResult.error(ValidationErrors.subElement(String.valueOf(index), result.getErrors()));
		});
	}

	public JsonArrayValidator require(int index) {
		addRule(ruleRequire(index));
		return this;
	}

	public JsonArrayValidator requireNonNull(int index) {
		addRule(ruleRequire(index)
			.bailAnd(ruleNonNull(index)));
		return this;
	}

	public JsonArrayValidator require(int index, JSONType type) {
		addRule(ruleRequire(index)
			.bailAnd(ruleIsOfType(index, type)));
		return this;
	}

	public JsonArrayValidator requireNonNull(int index, JSONType type) {
		addRule(ruleRequire(index)
			.bailAnd(ruleNonNull(index))
			.bailAnd(ruleIsOfType(index, type)));
		return this;
	}

	public JsonArrayValidator optional(int index, JSONType type) {
		addRule(ruleIsOfType(index, type));
		return this;
	}

	public JsonArrayValidator optionalNonNull(int index, JSONType type) {
		addRule(ruleNonNull(index)
			.bailAnd(ruleIsOfType(index, type)));
		return this;
	}

	public JsonArrayValidator requireEmail(int index) {
		addRule(ruleRequire(index)
			.bailAnd(ruleEmail(index)));
		return this;
	}

	public JsonArrayValidator optionalEmail(int index) {
		addRule(ruleEmail(index));
		return this;
	}

	public JsonArrayValidator requireEmailNonNull(int index) {
		addRule(ruleRequire(index)
			.bailAnd(ruleNonNull(index))
			.bailAnd(ruleEmail(index)));
		return this;
	}

	public JsonArrayValidator optionalEmailNonNull(int index) {
		addRule(ruleNonNull(index)
			.bailAnd(ruleEmail(index)));
		return this;
	}

	public JsonArrayValidator requireObject(int index, JsonObjectValidator validator) {
		addRule(ruleRequire(index)
			.bailAnd(ruleSubElementMatches(index, validator)));
		return this;
	}

	public JsonArrayValidator optionalObject(int index, JsonObjectValidator validator) {
		addRule(ruleSubElementMatches(index, validator));
		return this;
	}

	public JsonArrayValidator requireObjectNonNull(int index, JsonObjectValidator validator) {
		addRule(ruleRequire(index)
			.bailAnd(ruleNonNull(index))
			.bailAnd(ruleSubElementMatches(index, validator)));
		return this;
	}

	public JsonArrayValidator optionalObjectNonNull(int index, JsonObjectValidator validator) {
		addRule(ruleNonNull(index)
			.bailAnd(ruleSubElementMatches(index, validator)));
		return this;
	}

	public JsonArrayValidator requireArray(int index, JsonArrayValidator validator) {
		addRule(ruleRequire(index)
			.bailAnd(ruleSubElementMatches(index, validator)));
		return this;
	}

	public JsonArrayValidator optionalArray(int index, JsonArrayValidator validator) {
		addRule(ruleSubElementMatches(index, validator));
		return this;
	}

	public JsonArrayValidator requireArrayNonNull(int index, JsonArrayValidator validator) {
		addRule(ruleRequire(index)
			.bailAnd(ruleNonNull(index))
			.bailAnd(ruleSubElementMatches(index, validator)));
		return this;
	}

	public JsonArrayValidator optionalArrayNonNull(int index, JsonArrayValidator validator) {
		addRule(ruleNonNull(index)
			.bailAnd(ruleSubElementMatches(index, validator)));
		return this;
	}

	public JsonArrayValidator requireMinimumSize(int size) {
		addRule(a -> ValidationResult.check(a.size() >= size, "0", "Array must have at least " + size + " element(s)"));
		return this;
	}

	public JsonArrayValidator requireMaximumSize(int size) {
		addRule(a -> ValidationResult.check(a.size() <= size, "0", "Array must have at most " + size + " element(s)"));
		return this;
	}

	public JsonArrayValidator requireSize(int size) {
		addRule(a -> ValidationResult.check(a.size() == size, "0", "Array must have exactly " + size + " element(s)"));
		return this;
	}

	public JsonArrayValidator requireAllNonNull() {
		addRule(ruleAllNonNull());
		return this;
	}

	public JsonArrayValidator requireElementType(JSONType type) {
		addRule(ruleAllOfType(type));
		return this;
	}

	public JsonArrayValidator requireElementObjects(JsonObjectValidator validator) {
		addRule(ruleAllOfType(JSONType.OBJECT).and(a -> {
			ValidationResult result = ValidationResult.ok();
			for(int i = 0; i < a.size(); i++) {
				if(!a.isOfType(i, JSONType.OBJECT)) continue;
				JSONObject object = a.getJSONObject(i);
				if(object == null) return ValidationResult.ok();
				ValidationResult r = validator.validate(object);
				result = result.combine(r.asSubElement(String.valueOf(i)));
			}
			return result;
		}));
		return this;
	}

	public JsonArrayValidator requireElementArrays(JsonArrayValidator validator) {
		addRule(ruleAllOfType(JSONType.ARRAY).and(a -> {
			ValidationResult result = ValidationResult.ok();
			for(int i = 0; i < a.size(); i++) {
				if(!a.isOfType(i, JSONType.ARRAY)) continue;
				JSONArray array = a.getJSONArray(i);
				if(array == null) return ValidationResult.ok();
				ValidationResult r = validator.validate(array);
				result = result.combine(r.asSubElement(String.valueOf(i)));
			}
			return result;
		}));
		return this;
	}

}
