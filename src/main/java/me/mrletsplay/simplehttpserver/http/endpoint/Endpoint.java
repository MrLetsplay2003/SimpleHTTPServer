package me.mrletsplay.simplehttpserver.http.endpoint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Endpoint {

	public HttpRequestMethod method() default HttpRequestMethod.GET;

	public String path();

	public boolean pathPattern() default false;

}
