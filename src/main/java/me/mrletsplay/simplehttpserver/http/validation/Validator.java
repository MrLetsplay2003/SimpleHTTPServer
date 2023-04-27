package me.mrletsplay.simplehttpserver.http.validation;

import java.util.List;
import java.util.stream.Collectors;

import me.mrletsplay.simplehttpserver.http.validation.result.ValidationErrors;
import me.mrletsplay.simplehttpserver.http.validation.result.ValidationResult;

public interface Validator<T> {

	public void addRule(ValidationRule<T> rule);

	public List<ValidationRule<T>> getRules();

	public default ValidationResult validate(T data) {
		List<ValidationResult> results = getRules().stream()
			.map(r -> r.validate(data))
			.collect(Collectors.toList());

		if(results.stream().allMatch(r -> r.isOk())) return ValidationResult.ok();

		return ValidationResult.error(ValidationErrors.combine(results.stream()
			.filter(r -> !r.isOk())
			.map(r -> r.getErrors())
			.toArray(ValidationErrors[]::new)));
	}

}
