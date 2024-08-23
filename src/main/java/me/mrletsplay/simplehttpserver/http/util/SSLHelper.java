package me.mrletsplay.simplehttpserver.http.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import me.mrletsplay.mrcore.io.IOUtils;

public class SSLHelper {

	private SSLHelper() {}

	public static SSLContext createSSLContext(File certificateFile, File certificatePrivateKeyFile, String certificatePassword) throws FileNotFoundException, IOException, GeneralSecurityException {
		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(null);
		Certificate[] certificateChain = loadCertificateChain(certificateFile);
		for(int i = 0; i < certificateChain.length; i++) {
			keyStore.setCertificateEntry("certificate" + i, certificateChain[i]);
		}
		keyStore.setKeyEntry("certificateKey", loadCertificateKey(certificatePrivateKeyFile), certificatePassword != null ? certificatePassword.toCharArray() : new char[0], certificateChain);
		TrustManager[] trustManagers = createTrustManagers(keyStore);
		KeyManager[] keyManagers = createKeyManagers(keyStore, certificatePassword);

		SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
		sslContext.init(keyManagers, trustManagers, null);
		return sslContext;
	}

	private static X509TrustManager[] createTrustManagers(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
		TrustManagerFactory trustMgrFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustMgrFactory.init(keystore);
		TrustManager trustManagers[] = trustMgrFactory.getTrustManagers();
		for (int i = 0; i < trustManagers.length; i++) {
			if (trustManagers[i] instanceof X509TrustManager) {
				X509TrustManager[] tr = new X509TrustManager[1];
				tr[0] = (X509TrustManager) trustManagers[i];
				return tr;
			}
		}
		return null;
	}

	private static X509KeyManager[] createKeyManagers(KeyStore keystore, String certificatePassword) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
		KeyManagerFactory keyMgrFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyMgrFactory.init(keystore, certificatePassword != null ? certificatePassword.toCharArray() : new char[0]);
		KeyManager keyManagers[] = keyMgrFactory.getKeyManagers();
		for (int i = 0; i < keyManagers.length; i++) {
			if (keyManagers[i] instanceof X509KeyManager) {
				X509KeyManager[] kr = new X509KeyManager[1];
				kr[0] = (X509KeyManager) keyManagers[i];
				return kr;
			}
		}
		return null;
	}

	private static Certificate[] loadCertificateChain(File certFile) throws CertificateException, FileNotFoundException, IOException {
		CertificateFactory f = CertificateFactory.getInstance("X.509");
		try(FileInputStream in = new FileInputStream(certFile)) {
			return f.generateCertificates(in).toArray(Certificate[]::new);
		}
	}

	private static PrivateKey loadCertificateKey(File certKeyFile) throws FileNotFoundException, IOException, GeneralSecurityException {
		try(FileInputStream in = new FileInputStream(certKeyFile)) {
			return getPrivateKeyFromString(new String(IOUtils.readAllBytes(in), StandardCharsets.UTF_8));
		}
	}

	private static RSAPrivateKey getPrivateKeyFromString(String key) throws IOException, GeneralSecurityException {
		String privateKeyPEM = key;
		privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----", "");
		privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "");
		byte[] encoded = Base64.getDecoder().decode(privateKeyPEM.replace("\n", "").replace("\r", ""));
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
		RSAPrivateKey privKey = (RSAPrivateKey) kf.generatePrivate(keySpec);
		return privKey;
	}

}