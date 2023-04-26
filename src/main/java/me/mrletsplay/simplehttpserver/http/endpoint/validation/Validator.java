package me.mrletsplay.simplehttpserver.http.endpoint.validation;

import java.util.List;
import java.util.stream.Collectors;

public interface Validator<T> {

	public void addRule(ValidationRule<T> rule);

	public List<ValidationRule<T>> getRules();

	public default ValidationResult validate(T data) {
		List<ValidationResult> results = getRules().stream()
			.map(r -> r.validate(data))
			.collect(Collectors.toList());

		if(results.stream().allMatch(r -> r.isOk())) return ValidationResult.ok();

		return ValidationResult.error(results.stream()
			.filter(r -> !r.isOk())
			.flatMap(r -> r.getErrors().stream())
			.collect(Collectors.toList()));
	}

}
