package me.mrletsplay.simplehttpserver.http.validation.result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONObject;

public class ValidationErrors {

	private Map<String, List<String>> errors;
	private Map<String, ValidationErrors> subElements;

	public ValidationErrors(Map<String, List<String>> errors, Map<String, ValidationErrors> subElements) {
		this.errors = errors;
		this.subElements = subElements;
	}

	public Set<String> getErrorKeys() {
		return Collections.unmodifiableSet(errors.keySet());
	}

	public List<String> getErrors(String key) {
		return Collections.unmodifiableList(errors.get(key));
	}

	public Map<String, ValidationErrors> getSubElements() {
		return Collections.unmodifiableMap(subElements);
	}

	public ValidationErrors copy() {
		var newErrors = errors.entrySet().stream()
			.collect(Collectors.toMap(
				e -> e.getKey(),
				e -> (List<String>) new ArrayList<>(e.getValue()),
				(a, b) -> { a.addAll(b); return a; },
				HashMap::new));
		var newSubElements = subElements.entrySet().stream()
			.collect(Collectors.toMap(
				e -> e.getKey(),
				e -> e.getValue().copy(),
				(a, b) -> combine(a, b),
				HashMap::new));
		return new ValidationErrors(newErrors, newSubElements);
	}

	public JSONObject toJson() {
		JSONObject object = new JSONObject();
		errors.forEach((k, e) -> object.put(k, new JSONArray(e)));
		subElements.forEach((k, e) -> object.put(k, e.toJson()));
		return object;
	}

	public static ValidationErrors of(String key, String... messages) {
		if(messages.length == 0) throw new IllegalArgumentException("Need at least one error message");
		Map<String, List<String>> errors = new HashMap<>();
		errors.put(key, new ArrayList<>(List.of(messages)));
		return new ValidationErrors(errors, new HashMap<>());
	}

	public static ValidationErrors combine(ValidationErrors... errors) {
		// TODO: potentially check for collisions for keys in errors and subElements
		if(errors.length == 0) throw new IllegalArgumentException("Need at least one error");
		if(errors.length == 1) return errors[0];

		var newErrors = Arrays.stream(errors)
			.filter(Objects::nonNull)
			.flatMap(e -> e.errors.entrySet().stream())
			.collect(Collectors.toMap(
				e -> e.getKey(),
				e -> (List<String>) new ArrayList<>(e.getValue()),
				(a, b) -> { a.addAll(b); return a; },
				HashMap::new));
		var newSubElements = Arrays.stream(errors)
			.filter(Objects::nonNull)
			.flatMap(e -> e.subElements.entrySet().stream())
			.collect(Collectors.toMap(
				e -> e.getKey(),
				e -> e.getValue().copy(),
				(a, b) -> combine(a, b),
				HashMap::new));
		return new ValidationErrors(newErrors, newSubElements);
	}

	public static ValidationErrors subElement(String key, ValidationErrors errors) {
		Map<String, ValidationErrors> subElements = new HashMap<>();
		subElements.put(key, errors);
		return new ValidationErrors(new HashMap<>(), subElements);
	}

}
