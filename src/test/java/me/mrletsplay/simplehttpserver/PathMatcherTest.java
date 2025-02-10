package me.mrletsplay.simplehttpserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import me.mrletsplay.simplehttpserver.http.util.PathMatcher;

public class PathMatcherTest {

	@Test
	public void testMatchMatches() {
		assertEquals(Map.of("param1", "value"), PathMatcher.match("/a/{param1}/b", "/a/value/b"));
		assertEquals(Map.of("param1", "value"), PathMatcher.match("/a/b/{param1}/c/d", "/a/b/value/c/d"));
		assertEquals(Map.of("param1", "value"), PathMatcher.match("/a/pre{param1}suf/b", "/a/prevaluesuf/b"));
		assertEquals(Map.of("param1", "value", "param2", "x"), PathMatcher.match("/a/{param1}/{param2}", "/a/value/x"));
	}

	@Test
	public void testMatchDoesntMatch() {
		assertEquals(null, PathMatcher.match("/a/b/{param1}", "/a/b/value/c/d"));
		assertEquals(null, PathMatcher.match("/a/{param1}/b", "/a/value/x"));
	}

	@Test
	public void testMatchVariable() {
		// Variable parameter
		assertEquals(Map.of("param1", "c"), PathMatcher.match("/a/b/{param1...}", "/a/b/c"));
		assertEquals(Map.of("param1", "value/value2"), PathMatcher.match("/a/b/{param1...}", "/a/b/value/value2"));
	}

	@Test
	public void testMatchOptional() {
		// Optional parameter
		Map<String, String> out = new HashMap<>();
		out.put("param1", null);
		assertEquals(out, PathMatcher.match("/a/p{param1?}/b", "/a/p/b"));
	}

	@Test
	public void testMatchInvalidPattern() {
		// Invalid path patterns
		assertThrows(IllegalArgumentException.class, () -> PathMatcher.match("/a/{}/b", "/a/value/b"));
		assertThrows(IllegalArgumentException.class, () -> PathMatcher.match("/a/{?...}/b", "/a/value/b"));
		assertThrows(IllegalArgumentException.class, () -> PathMatcher.match("/a/}test{/b", "/a/value/b"));
		assertThrows(IllegalArgumentException.class, () -> PathMatcher.match("/a/{param1?}/b", "/a/value/b"));
		assertThrows(IllegalArgumentException.class, () -> PathMatcher.match("/a/{test1}{test2}/b", "/a/value/b"));
		assertThrows(IllegalArgumentException.class, () -> PathMatcher.match("/a/{test1}b{test2}/c", "/a/value/b"));
	}

}
