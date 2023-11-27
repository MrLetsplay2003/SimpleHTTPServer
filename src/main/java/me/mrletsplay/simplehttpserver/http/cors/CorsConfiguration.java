package me.mrletsplay.simplehttpserver.http.cors;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;

/**
 * Configuration for CORS
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS">https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS</a>
 */
public class CorsConfiguration {

	private boolean allowAllOrigins;
	private Set<String> allowedOrigins;
	private Set<String> exposedHeaders;
	private int maxAge;
	private boolean allowCredentials;
	private boolean sendAllAllowedMethods;
	private Set<HttpRequestMethod> allowedMethods;
	private Set<String> allowedHeaders;

	private CorsConfiguration() {
		this.allowAllOrigins = false;
		this.allowedOrigins = new LinkedHashSet<>();
		this.exposedHeaders = new LinkedHashSet<>();
		this.maxAge = 5;
		this.allowCredentials = false;
		this.sendAllAllowedMethods = false;
		this.allowedMethods = new LinkedHashSet<>();
		this.allowedHeaders = new LinkedHashSet<>();
	}

	/**
	 * Unconditionally allows all origins by setting the allowed origins to the request's origin.<br>
	 * Overrides any configuration using {@link #addAllowedOrigins(String...)}
	 * @param allowAllOrigins Whether to allow all origins
	 * @return This configuration
	 */
	public CorsConfiguration allowAllOrigins(boolean allowAllOrigins) {
		this.allowAllOrigins = allowAllOrigins;
		return this;
	}

	public boolean isAllowAllOrigins() {
		return allowAllOrigins;
	}

	/**
	 * Adds origins to the list of allowed origins
	 * @param origins The origins to allow
	 * @return This configuration
	 */
	public CorsConfiguration addAllowedOrigins(String... origins) {
		this.allowedOrigins.addAll(Arrays.asList(origins));
		return this;
	}

	/**
	 * Removes origins previously added using {@link #addAllowedOrigins(String...)}
	 * @param origins The origins to disallow
	 * @return This configuration
	 */
	public CorsConfiguration removeAllowedOrigins(String... origins) {
		this.allowedOrigins.removeAll(Arrays.asList(origins));
		return this;
	}

	public Set<String> getAllowedOrigins() {
		return Collections.unmodifiableSet(allowedOrigins);
	}

	/**
	 * Adds headers to the list of exposed headers (can be accessed by JavaScript)
	 * @param exposedHeaders The headers to expose
	 * @return This configuration
	 */
	public CorsConfiguration addExposedHeaders(String... exposedHeaders) {
		this.exposedHeaders.addAll(Arrays.asList(exposedHeaders));
		return this;
	}

	/**
	 * Removes exposed headers previously added using {@link #addExposedHeaders(String...)}
	 * @param exposedHeaders The headers to not expose
	 * @return This configuration
	 */
	public CorsConfiguration removeExposedHeaders(String... exposedHeaders) {
		this.exposedHeaders.removeAll(Arrays.asList(exposedHeaders));
		return this;
	}

	public Set<String> getExposedHeaders() {
		return Collections.unmodifiableSet(exposedHeaders);
	}

	/**
	 * Sets the max age for cached preflight responses
	 * @param maxAge The max age
	 * @return This configuration
	 */
	public CorsConfiguration maxAge(int maxAge) {
		this.maxAge = maxAge;
		return this;
	}

	public int getMaxAge() {
		return maxAge;
	}

	/**
	 * Allows credentials to be sent
	 * @param allowCredentials Whether to allow credentials
	 * @return This configuration
	 */
	public CorsConfiguration allowCredentials(boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
		return this;
	}

	public boolean isAllowCredentials() {
		return allowCredentials;
	}

	/**
	 * Always send all allowed methods to the client, not just the ones for which a document is registered under the requested path
	 * @param sendAllAllowedMethods Whether to send all allowed methods
	 * @return This configuration
	 */
	public CorsConfiguration sendAllAllowedMethods(boolean sendAllAllowedMethods) {
		this.sendAllAllowedMethods = sendAllAllowedMethods;
		return this;
	}

	public boolean isSendAllAllowedMethods() {
		return sendAllAllowedMethods;
	}

	/**
	 * Adds methods to the list of allowed methods<br>
	 * By default, this will be reduced to only allow the methods for which a document is registered under the requested path. You can change this using {@link #sendAllAllowedMethods(boolean)}
	 * @param methods The methods to allow
	 * @return This configuration
	 */
	public CorsConfiguration addAllowedMethods(HttpRequestMethod... methods) {
		this.allowedMethods.addAll(Arrays.asList(methods));
		return this;
	}

	/**
	 * Removes methods from the list of allowed methods
	 * @param methods The methods to not allow
	 * @return This configuration
	 */
	public CorsConfiguration removeAllowedMethods(HttpRequestMethod... methods) {
		this.allowedMethods.removeAll(Arrays.asList(methods));
		return this;
	}

	public Set<HttpRequestMethod> getAllowedMethods() {
		return Collections.unmodifiableSet(allowedMethods);
	}

	/**
	 * Adds headers to the list of allowed headers
	 * @param allowedHeaders The headers to allow
	 * @return This configuration
	 */
	public CorsConfiguration addAllowedHeaders(String... allowedHeaders) {
		this.allowedHeaders.addAll(Arrays.asList(allowedHeaders));
		return this;
	}

	/**
	 * Removes headers previously added using {@link #addAllowedHeaders(String...)}
	 * @param allowedHeaders The headers to not allow
	 * @return This configuration
	 */
	public CorsConfiguration removeAllowedHeader(String... allowedHeaders) {
		this.allowedHeaders.removeAll(Arrays.asList(allowedHeaders));
		return this;
	}

	public Set<String> getAllowedHeaders() {
		return Collections.unmodifiableSet(allowedHeaders);
	}

	/**
	 * Creates a configuration where all request methods are allowed, but no other origins. Also doesn't expose or allow any headers and doesn't allow credentials.
	 * @return A CORS configuration
	 */
	public static CorsConfiguration createDefault() {
		return new CorsConfiguration()
			.addAllowedMethods(HttpRequestMethod.values());
	}

	/**
	 * Creates a configuration where requests from all origins are unconditionally allowed using all methods. This does not include exposing all headers
	 * @return A CORS configuration
	 */
	public static CorsConfiguration createAllowAll() {
		return new CorsConfiguration()
			.addAllowedMethods(HttpRequestMethod.values())
			.allowAllOrigins(true)
			.addAllowedHeaders("*")
			.allowCredentials(true);
	}

}
