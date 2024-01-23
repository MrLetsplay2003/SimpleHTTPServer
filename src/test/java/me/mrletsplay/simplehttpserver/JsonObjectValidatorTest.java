package me.mrletsplay.simplehttpserver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import me.mrletsplay.mrcore.json.JSONObject;
import me.mrletsplay.mrcore.json.JSONType;
import me.mrletsplay.simplehttpserver.http.validation.JsonArrayValidator;
import me.mrletsplay.simplehttpserver.http.validation.JsonObjectValidator;

public class JsonObjectValidatorTest {

	@Test
	public void testRequire() {
		JsonObjectValidator validator = new JsonObjectValidator()
			.require("firstName")
			.require("lastName", JSONType.STRING);

		assertTrue(validator.validate(new JSONObject("{\"firstName\":\"Test\",\"lastName\":\"Test\"}")).isOk());

		assertFalse(validator.validate(new JSONObject()).isOk());
		assertFalse(validator.validate(new JSONObject("{\"firstName\":\"Test\"}")).isOk());
		assertFalse(validator.validate(new JSONObject("{\"firstName\":\"Test\",\"lastName\":0}")).isOk());
	}

	@Test
	public void testOptional() {
		JsonObjectValidator validator = new JsonObjectValidator()
			.optional("firstName", JSONType.STRING);

		assertTrue(validator.validate(new JSONObject()).isOk());
		assertTrue(validator.validate(new JSONObject("{\"firstName\":\"Test\"}")).isOk());

		assertFalse(validator.validate(new JSONObject("{\"firstName\":0}")).isOk());
	}

	@Test
	public void testEmail() {
		JsonObjectValidator validator = new JsonObjectValidator()
			.requireEmail("email");

		assertTrue(validator.validate(new JSONObject("{\"email\":\"alice@example.com\"}")).isOk());
		assertFalse(validator.validate(new JSONObject("{\"email\":\"not an email\"}")).isOk());
		assertFalse(validator.validate(new JSONObject("{\"email\":false}")).isOk());
	}

	@Test
	public void testObject() {
		JsonObjectValidator validator = new JsonObjectValidator()
			.requireObjectNonNull("data", new JsonObjectValidator()
				.require("number", JSONType.DECIMAL));

		assertTrue(validator.validate(new JSONObject("{\"data\":{\"number\":1.0}}")).isOk());
		assertFalse(validator.validate(new JSONObject("{\"data\":null}")).isOk());
		assertFalse(validator.validate(new JSONObject("{\"data\":{}}")).isOk());
		assertFalse(validator.validate(new JSONObject("{\"data\":{\"number\":false}}")).isOk());
	}

	@Test
	public void testArray() {
		JsonObjectValidator validator = new JsonObjectValidator()
			.requireArray("data", new JsonArrayValidator()
				.require(0, JSONType.DECIMAL));

		assertTrue(validator.validate(new JSONObject("{\"data\":[1.0]}")).isOk());
		assertTrue(validator.validate(new JSONObject("{\"data\":null}")).isOk());
		assertFalse(validator.validate(new JSONObject("{\"data\":[]}")).isOk());
		assertFalse(validator.validate(new JSONObject("{\"data\":[false]}")).isOk());
	}

}
