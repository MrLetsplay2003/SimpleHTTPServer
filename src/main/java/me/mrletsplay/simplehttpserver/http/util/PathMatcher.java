package me.mrletsplay.simplehttpserver.http.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PathMatcher {

	/**
	 * Tests a path pattern against a specific path to see whether it matches.<br>
	 * <br>
	 * A pattern matched path usually looks like this:<br>
	 * <code>/path/{parameter1}/more/{parameter2}</code><br>
	 * where <code>parameter1</code> and <code>parameter2</code> are path parameters that will be provided when a request that matches the pattern is recieved.<br>
	 * <ul>
	 * <li>Optional parameters may be specified using <code>?</code> after the parameter name<br></li>
	 * <li>Variable parameters may be specified using <code>...</code> after the parameter name<br></li>
	 * <li>Variable parameters may only be used in the last path segment</li>
	 * </ul>
	 * <br>
	 * Example inputs and outputs:
	 * <table>
	 * <tr><th>Pattern</th><th>Path</th><th>Output</th>
	 * <tr><td>/path/{parameter1}/more/{parameter2}</td><td>/path/one/more/two</td><td>{parameter1=one, parameter2=two}</td></tr>
	 * <tr><td>/path/{parameter1}/more/{parameter2...}</td><td>/path/one/more/two/three</td><td>{parameter1=one, parameter2=two/three}</td></tr>
	 * <tr><td>/path/pre{parameter1}suf/more</td><td>/path/prevalsuf/more</td><td>{parameter1=val}</td></tr>
	 * <tr><td>/path/pre{parameter1?}suf/more</td><td>/path/presuf/more</td><td>{parameter1=null}</td></tr>
	 * <tr><td>/path/pre{parameter1}suf/more</td><td>/path/presuf/more</td><td>null</td></tr>
	 * </table>
	 * @param pattern The path pattern to match agains
	 * @param path The path to match
	 * @return A map of key-value pairs of the matched parameters or <code>null</code> if it wasn't matched
	 */
	public static Map<String, String> match(String pattern, String path) {
		Map<String, String> params = new LinkedHashMap<>();

		String[] parts = path.split("/", -1);
		String[] patternParts = pattern.split("/", -1);

		if(parts.length < patternParts.length) return null;

		for(int i = 0; i < patternParts.length; i++) {
			String pp = patternParts[i];
			String p = parts[i];

			if(pp.contains("{")) {
				if(!pp.contains("}")) throw new IllegalArgumentException("Unclosed path parameter");
				int paramStart = pp.indexOf('{');
				int paramEnd = pp.indexOf('}');
				if(paramEnd < paramStart) throw new IllegalArgumentException("Malformed parameter");
				String prefix = pp.substring(0, paramStart);
				String suffix = pp.substring(paramEnd + 1);

				String paramName = pp.substring(paramStart + 1, paramEnd);
				boolean optional = false;
				boolean variable = false;
				if(paramName.endsWith("...")) {
					paramName = paramName.substring(0, paramName.length() - 3);
					variable = true;
				}

				if(paramName.endsWith("?")) {
					paramName = paramName.substring(0, paramName.length() - 1);
					optional = true;
				}

				if(paramName.isEmpty()) {
					throw new IllegalArgumentException("Parameter has no name");
				}

				if(variable && optional) {
					throw new IllegalArgumentException("Parameter may not be both variable and optional");
				}

				if(variable && i != patternParts.length - 1) {
					throw new IllegalArgumentException("Only the last parameter may be variable");
				}

				if(optional && prefix.isEmpty() && suffix.isEmpty()) {
					throw new IllegalArgumentException("Optional parameter may not be on its own");
				}

				if(variable) {
					p = Arrays.stream(parts).skip(i).collect(Collectors.joining("/"));
				}

				if(p.length() < (prefix + suffix).length()) return null; // Avoid cases where prefix and suffix overlap
				if(!p.startsWith(prefix) || !p.endsWith(suffix)) return  null;
				if(p.length() == (prefix + suffix).length() && !optional) return null; // Empty parameter, just prefix and suffix, but not optional

				String val = p.substring(prefix.length(), p.length() - suffix.length());
				params.put(paramName, val.isEmpty() ? null : val);
				continue;
			}

			if(!pp.equals(p)) return null;
		}

		return params;
	}

}
