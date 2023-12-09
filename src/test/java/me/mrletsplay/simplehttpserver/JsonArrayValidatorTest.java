package me.mrletsplay.simplehttpserver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import me.mrletsplay.mrcore.json.JSONArray;
import me.mrletsplay.mrcore.json.JSONType;
import me.mrletsplay.simplehttpserver.http.validation.JsonArrayValidator;
import me.mrletsplay.simplehttpserver.http.validation.JsonObjectValidator;

public class JsonArrayValidatorTest {

	@Test
	public void testRequire() {
		JsonArrayValidator validator = new JsonArrayValidator()
			.require(0)
			.require(1, JSONType.STRING);

		assertTrue(validator.validate(new JSONArray("[\"Test\", \"Test\"]")).isOk());

		assertFalse(validator.validate(new JSONArray()).isOk());
		assertFalse(validator.validate(new JSONArray("[\"Test\"]")).isOk());
		assertFalse(validator.validate(new JSONArray("[\"Test\", 0]")).isOk());
	}

	@Test
	public void testOptional() {
		JsonArrayValidator validator = new JsonArrayValidator()
			.optional(0, JSONType.STRING);

		assertTrue(validator.validate(new JSONArray()).isOk());
		assertTrue(validator.validate(new JSONArray("[\"Test\"]")).isOk());

		assertFalse(validator.validate(new JSONArray("[0]")).isOk());
	}

	@Test
	public void testEmail() {
		JsonArrayValidator validator = new JsonArrayValidator()
			.requireEmail(0);

		assertTrue(validator.validate(new JSONArray("[\"alice@example.com\"]")).isOk());
		assertFalse(validator.validate(new JSONArray("[\"not an email\"]")).isOk());
		assertFalse(validator.validate(new JSONArray("[false]")).isOk());
	}

	@Test
	public void testObject() {
		JsonArrayValidator validator = new JsonArrayValidator()
			.requireObject(0, new JsonObjectValidator()
				.require("number", JSONType.DECIMAL));

		assertTrue(validator.validate(new JSONArray("[{\"number\":1.0}]")).isOk());
		assertFalse(validator.validate(new JSONArray("[{}]")).isOk());
		assertFalse(validator.validate(new JSONArray("[{\"number\":false}]")).isOk());
	}

	@Test
	public void testArray() {
		JsonArrayValidator validator = new JsonArrayValidator()
			.requireArray(0, new JsonArrayValidator()
				.require(0, JSONType.DECIMAL));

		assertTrue(validator.validate(new JSONArray("[[1.0]]")).isOk());
		assertFalse(validator.validate(new JSONArray("[]")).isOk());
		assertFalse(validator.validate(new JSONArray("[[]]")).isOk());
		assertFalse(validator.validate(new JSONArray("[[false]]")).isOk());
	}

}
