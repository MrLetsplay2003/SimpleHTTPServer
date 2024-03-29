package me.mrletsplay.simplehttpserver.php;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import me.mrletsplay.mrcore.io.IOUtils;
import me.mrletsplay.simplehttpserver.http.HttpRequestMethod;
import me.mrletsplay.simplehttpserver.http.HttpStatusCode;
import me.mrletsplay.simplehttpserver.http.document.HttpDocument;
import me.mrletsplay.simplehttpserver.http.request.HttpRequestContext;
import me.mrletsplay.simplehttpserver.http.util.MimeType;

public class PHPFileDocument implements HttpDocument {

	private Path path;
	private MimeType fallbackMimeType;

	/**
	 * @deprecated Use {@link #PHPFileDocument(Path, MimeType)} instead
	 * @param file
	 * @param fallbackMimeType
	 */
	@Deprecated
	public PHPFileDocument(File file, MimeType fallbackMimeType) {
		this.path = file.toPath();
		this.fallbackMimeType = fallbackMimeType;
	}

	public PHPFileDocument(Path path, MimeType fallbackMimeType) {
		this.path = path;
		this.fallbackMimeType = fallbackMimeType;
	}

	/**
	 * @deprecated Use {@link #PHPFileDocument(Path)} instead
	 * @param file
	 */
	@Deprecated
	public PHPFileDocument(File file) {
		this(file, MimeType.HTML);
	}

	/**
	 * Creates a PHP document with {@link MimeType#HTML} as its fallback MIME type
	 * @param path
	 */
	public PHPFileDocument(Path path) {
		this(path, MimeType.HTML);
	}

	@Override
	public void createContent() {
		HttpRequestContext c = HttpRequestContext.getCurrentContext();
		if(!PHP.isEnabled()) {
			c.getServerHeader().setContent(MimeType.TEXT, "PHP is disabled".getBytes(StandardCharsets.UTF_8));
			return;
		}

		List<String> cmd = new ArrayList<>();
		cmd.add(PHP.getCGIPath());

		ProcessBuilder b = new ProcessBuilder(cmd);
		b.redirectError(Redirect.INHERIT);

		Map<String, String> env = b.environment();
		env.put("REDIRECT_STATUS", "CGI");
		env.put("REQUEST_METHOD", c.getClientHeader().getMethod().name());
		env.put("SCRIPT_NAME", c.getClientHeader().getPath().getDocumentPath());
		env.put("SCRIPT_FILENAME", path.toAbsolutePath().normalize().toString());
		env.put("REQUEST_URI", c.getClientHeader().getPath().getDocumentPath());
		env.put("QUERY_STRING", c.getClientHeader().getPath().getQuery().toString());
		if(c.getClientHeader().getMethod() == HttpRequestMethod.POST
				&& !c.getClientHeader().getFields().getAll("Content-Type").isEmpty()) {
				env.put("CONTENT_TYPE", c.getClientHeader().getFields().getFirst("Content-Type"));
		}
		env.put("CONTENT_LENGTH", ""+c.getClientHeader().getPostData().getRaw().length);

		c.getClientHeader().getFields().getRaw().forEach((k, v) -> {
			env.put("HTTP_" + k.replace("-", "_").toUpperCase(), v.stream().collect(Collectors.joining("; ")));
		});

		env.put("SERVER_NAME", "localhost");
		env.put("SERVER_ADDR", "127.0.0.1");
		env.put("SERVER_PORT", "80");
		env.put("GATEWAY_INTERFACE", "CGI/1.1");
		env.put("SERVER_PROTOCOL", "HTTP/1.1");
		String pth = Paths.get("").toAbsolutePath().toString();
		env.put("DOCUMENT_ROOT", pth);

		try {
			Process p = b.start();
			ByteArrayInputStream bin = new ByteArrayInputStream(c.getClientHeader().getPostData().getRaw());
			IOUtils.transfer(bin, p.getOutputStream());
			p.getOutputStream().flush();
			bin.close();

			byte[] data = IOUtils.readAllBytes(p.getInputStream());

			String dt = new String(data, StandardCharsets.UTF_8);
			dt = dt.substring(0, dt.indexOf("\r\n\r\n"));

			int prDtLen = dt.getBytes(StandardCharsets.UTF_8).length;
			byte[] postData = new byte[data.length - prDtLen - 4]; // - 4 bytes because of the \r\n\r\n

			System.arraycopy(data, prDtLen + 4, postData, 0, postData.length);

			for(String h : dt.split("\r\n")) {
				String[] spl = h.split(": ", 2);
				if(spl[0].equalsIgnoreCase("Status")) {
					c.getServerHeader().setStatusCode(new HttpStatusCode() {

						@Override
						public String getStatusMessage() {
							return spl[1].split(" ", 2)[1];
						}

						@Override
						public int getStatusCode() {
							return Integer.parseInt(spl[1].split(" ", 2)[0]);
						}
					});
					continue;
				}
				if(spl[0].equalsIgnoreCase("Content-Type")) {
					c.getServerHeader().getFields().set(spl[0], spl[1]); // Always *override* content type
					continue;
				}
				c.getServerHeader().getFields().add(spl[0], spl[1]);
			}
			c.getServerHeader().setContent(fallbackMimeType, postData, false);
		} catch (IOException e) {
			PHP.getLogger().error("Error while running PHP-CGI", e);
		}
	}

}