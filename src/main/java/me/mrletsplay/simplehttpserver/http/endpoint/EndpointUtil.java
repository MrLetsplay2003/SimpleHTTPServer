package me.mrletsplay.simplehttpserver.http.endpoint;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;

import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;

public class EndpointUtil {

	private EndpointUtil() {}

	/**
	 * Tries to find an {@link Endpoint @Endpoint} annotation for a method, also searching overridden methods in superclasses.<br>
	 * If an endpoint is annotated with the {@link UnavailableEndpoint @UnavailableEndpoint} annotation, this method will return <code>null</code>, however, this annotation is not inherited to overridden methods.
	 * @param method The method
	 * @return The annotation or <code>null</code> if there is no method with the annotation
	 * @throws SecurityException If the exception occurs during reflection
	 */
	public static Endpoint getEndpoint(Method method) throws SecurityException {
		if(method.isAnnotationPresent(UnavailableEndpoint.class)) return null;

		while(true) {
			Endpoint endpoint = method.getAnnotation(Endpoint.class);
			if(endpoint != null) return endpoint;

			try {
				Class<?> superClass = method.getDeclaringClass().getSuperclass();
				if(superClass == null) return null;
				method = superClass.getMethod(method.getName(), method.getParameterTypes());
			} catch (NoSuchMethodException e) {
				return null;
			}
		}
	}

	/**
	 * Tries to find a {@link RequestParameter @RequestParameter} annotation for every parameter of a method, also searching overridden methods in superclasses.<br>
	 * The annotations may be "spread" across multiple classes, not all of them have to be declared in the same class.<br>
	 * For every parameter, the "most recent" annotation (that is, the annotation of the parameter of the method in the class which is the furthest down the inheritance tree) is used.
	 * @param method The method
	 * @return An array of annotations or <code>null</code> if one or more of the parameters is missing the annotation
	 * @throws SecurityException If the exception occurs during reflection
	 */
	public static RequestParameter[] getRequestParameters(Method method) throws SecurityException {
		boolean hasContext = method.getParameterCount() > 0 && method.getParameterTypes()[0].equals(HttpRequestContext.class);
		int paramOffset = hasContext ? -1 : 0;
		RequestParameter[] requestParams = new RequestParameter[method.getParameterCount() + paramOffset];

		while(true) {
			Parameter[] params = method.getParameters();
			for(int i = 0; i < params.length; i++) {
				if(i < -paramOffset
					|| requestParams[i + paramOffset] != null
					|| params[i].getType().equals(HttpRequestContext.class)) continue;
				requestParams[i + paramOffset] = params[i].getAnnotation(RequestParameter.class);
			}

			if(!Arrays.asList(requestParams).contains(null)) return requestParams;

			try {
				Class<?> superClass = method.getDeclaringClass().getSuperclass();
				if(superClass == null) return null;
				method = superClass.getMethod(method.getName(), method.getParameterTypes());
			} catch (NoSuchMethodException e) {
				return null;
			}
		}
	}

}
