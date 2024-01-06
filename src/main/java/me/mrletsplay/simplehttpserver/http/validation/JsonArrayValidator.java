package me.mrletsplay.simplehttpserver.http.validation;

import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONType;
import me.mrletsplay.simplehttpserver.http.validation.result.ValidationErrors;
import me.mrletsplay.simplehttpserver.http.validation.result.ValidationResult;

public class JsonArrayValidator extends AbstractValidator<JSONArray> {

	private static ValidationRule<JSONArray> ruleRequire(int index) {
		return a -> ValidationResult.check(a.has(index), String.valueOf(index), "Missing element");
	}

	private static ValidationRule<JSONArray> ruleIsOfType(int index, JSONType type) {
		return a -> {
			if(!a.has(index)) return ValidationResult.ok();
			return ValidationResult.check(a.isOfType(index, type), String.valueOf(index), "Element must be of type " + type);
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
			return ValidationResult.check(ValidationUtil.isEmail(a.getString(index)), String.valueOf(index), "Not an email address");
		});
	}

	private static ValidationRule<JSONArray> ruleSubElementMatches(int index, JsonObjectValidator validator) {
		return ruleIsOfType(index, JSONType.OBJECT).bailAnd(a -> {
			if(!a.has(index)) return ValidationResult.ok();
			ValidationResult result = validator.validate(a.getJSONObject(index));
			if(result.isOk()) return result;
			return ValidationResult.error(ValidationErrors.subElement(String.valueOf(index), result.getErrors()));
		});
	}

	private static ValidationRule<JSONArray> ruleSubElementMatches(int index, JsonArrayValidator validator) {
		return ruleIsOfType(index, JSONType.ARRAY).bailAnd(a -> {
			if(!a.has(index)) return ValidationResult.ok();
			ValidationResult result = validator.validate(a.getJSONArray(index));
			if(result.isOk()) return result;
			return ValidationResult.error(ValidationErrors.subElement(String.valueOf(index), result.getErrors()));
		});
	}

	public JsonArrayValidator require(int index) {
		addRule(ruleRequire(index));
		return this;
	}

	public JsonArrayValidator require(int index, JSONType type) {
		addRule(ruleRequire(index)
			.bailAnd(ruleIsOfType(index, type)));
		return this;
	}

	public JsonArrayValidator optional(int index, JSONType type) {
		addRule(ruleIsOfType(index, type));
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

	public JsonArrayValidator requireObject(int index, JsonObjectValidator validator) {
		addRule(ruleRequire(index)
			.bailAnd(ruleSubElementMatches(index, validator)));
		return this;
	}

	public JsonArrayValidator optionalObject(int index, JsonObjectValidator validator) {
		addRule(ruleSubElementMatches(index, validator));
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

	public JsonArrayValidator requireElementType(JSONType type) {
		addRule(ruleAllOfType(type));
		return this;
	}

	public JsonArrayValidator requireElementObjects(JsonObjectValidator validator) {
		addRule(ruleAllOfType(JSONType.OBJECT).and(a -> {
			ValidationResult result = ValidationResult.ok();
			for(int i = 0; i < a.size(); i++) {
				if(!a.isOfType(i, JSONType.OBJECT)) continue;
				ValidationResult r = validator.validate(a.getJSONObject(i));
				result = result.combine(r);
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
				ValidationResult r = validator.validate(a.getJSONArray(i));
				result = result.combine(r);
			}
			return result;
		}));
		return this;
	}

}
