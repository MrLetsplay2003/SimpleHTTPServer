package me.mrletsplay.simplehttpserver.http.endpoint.validation;

import java.util.ArrayList;
import java.util.List;

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

			List<String> errors = new ArrayList<>(result.getErrors());
			errors.addAll(oResult.getErrors());
			return ValidationResult.error(errors);
		};
	}

}
