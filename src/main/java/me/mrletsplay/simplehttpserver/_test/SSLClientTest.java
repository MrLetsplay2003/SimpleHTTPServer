package me.mrletsplay.simplehttpserver._test;

import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class SSLClientTest {

	public static void main(String[] args) throws Exception {
		SSLContext ctx = SSLContext.getInstance("TLSv1.3");
		ctx.init(new KeyManager[] {}, new TrustManager[] {new X509TrustManager() {

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[0];
			}

		}}, new SecureRandom());

		Socket ss = ctx.getSocketFactory().createSocket("localhost", 8080);
		ss.getOutputStream().write("GET / HTTP/1.1\r\n\r\n".getBytes());
		while(true) {
			byte[] b = new byte[1024];
			int len;
			if((len = ss.getInputStream().read(b)) > 0) {
				System.out.println(len);
				System.out.println(new String(b));
			}

		}
	}

}
