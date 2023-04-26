package me.mrletsplay.simplehttpserver.http.endpoint.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbstractValidator<T> implements Validator<T> {

	private List<ValidationRule<T>> rules;

	public AbstractValidator() {
		this.rules = new ArrayList<>();
	}

	@Override
	public void addRule(ValidationRule<T> rule) {
		rules.add(rule);
	}

	@Override
	public List<ValidationRule<T>> getRules() {
		return Collections.unmodifiableList(rules);
	}

}
