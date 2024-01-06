package me.mrletsplay.simplehttpserver.http.validation;

import java.util.regex.Pattern;

public class ValidationUtil {

	private static final Pattern EMAIL_REGEX = Pattern.compile("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");

	private ValidationUtil() {}

	/**
	 * Checks whether a string looks like an email address based on a simple regex.<br>
	 * <br>
	 * Note: This method is not fully RFC compliant and may incorrectly classify malformed email addresses as valid. To make sure the email address is actually valid, just send an email to it
	 * @param str The string to check
	 * @return Whether the string looks like an email address
	 */
	public static boolean isEmail(String str) {
		return EMAIL_REGEX.matcher(str).matches();
	}

}
