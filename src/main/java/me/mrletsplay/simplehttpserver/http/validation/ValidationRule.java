package me.mrletsplay.simplehttpserver.http.validation;

import me.mrletsplay.simplehttpserver.http.validation.result.ValidationErrors;
import me.mrletsplay.simplehttpserver.http.validation.result.ValidationResult;

@FunctionalInterface
public interface ValidationRule<T> {

	public ValidationResult validate(T data);

	public default ValidationRule<T> bailAnd(ValidationRule<T> other) {
		return data -> {
			ValidationResult result = validate(data);
			if(!result.isOk()) return result;
			return other.validate(data);
		};
	}

	public default ValidationRule<T> and(ValidationRule<T> other) {
		return data -> {
			ValidationResult result = validate(data);
			ValidationResult oResult = other.validate(data);

			if(result.isOk() && oResult.isOk()) return ValidationResult.ok();
			if(result.isOk()) return oResult;
			if(oResult.isOk()) return result;

			return ValidationResult.error(ValidationErrors.combine(result.getErrors(), oResult.getErrors()));
		};
	}

}
