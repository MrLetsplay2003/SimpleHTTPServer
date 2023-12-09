package me.mrletsplay.simplehttpserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import me.mrletsplay.simplehttpserver.http.util.PathMatcher;

public class PathMatcherTest {

	@Test
	public void testMatch() {
		Map<String, String> out = new HashMap<>();

		out.put("param1", "value");
		assertEquals(out, PathMatcher.match("/a/{param1}/b", "/a/value/b"));
		assertEquals(out, PathMatcher.match("/a/pre{param1}suf/b", "/a/prevaluesuf/b"));
		assertEquals(null, PathMatcher.match("/a/{param1}/b", "/a/value/x"));
		out.put("param2", "x");
		assertEquals(out, PathMatcher.match("/a/{param1}/{param2}", "/a/value/x"));

		// Variable parameter
		out.clear();
		out.put("param1", "c");
		assertEquals(out, PathMatcher.match("/a/b/{param1...}", "/a/b/c"));
		out.put("param1", "value/value2");
		assertEquals(out, PathMatcher.match("/a/b/{param1...}", "/a/b/value/value2"));

		// Optional parameter
		out.put("param1", null);
		assertEquals(out, PathMatcher.match("/a/p{param1?}/b", "/a/p/b"));

		// Invalid path patterns
		assertThrows(IllegalArgumentException.class, () -> PathMatcher.match("/a/{}/b", "/a/value/b"));
		assertThrows(IllegalArgumentException.class, () -> PathMatcher.match("/a/{?...}/b", "/a/value/b"));
		assertThrows(IllegalArgumentException.class, () -> PathMatcher.match("/a/}test{/b", "/a/value/b"));
		assertThrows(IllegalArgumentException.class, () -> PathMatcher.match("/a/{param1?}/b", "/a/value/b"));
	}

}
