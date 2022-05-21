package me.mrletsplay.simplehttpserver.php;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PHP {

	private static final Logger LOGGER = LoggerFactory.getLogger(PHP.class);

	private static boolean enabled;
	private static String cgiPath;
	private static List<String> fileExtensions;

	static {
		enabled = false;
		cgiPath = "php-cgi";
		fileExtensions = Arrays.asList(".php");
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean enabled) {
		PHP.enabled = enabled;
	}

	public static String getCGIPath() {
		return cgiPath;
	}

	public static void setCGIPath(String cgiPath) {
		PHP.cgiPath = cgiPath;
	}

	public static void setFileExtensions(List<String> fileExtensions) {
		PHP.fileExtensions = fileExtensions;
	}

	public static List<String> getFileExtensions() {
		return fileExtensions;
	}

	public static Logger getLogger() {
		return LOGGER;
	}

}
