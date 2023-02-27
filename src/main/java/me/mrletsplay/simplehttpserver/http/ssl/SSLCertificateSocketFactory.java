package me.mrletsplay.simplehttpserver.http.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
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
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import me.mrletsplay.mrcore.io.IOUtils;

public class SSLCertificateSocketFactory {

	private File
		certificateFile,
		certificatePrivateKeyFile;

	private String certificatePassword;

	private boolean isP7b;

	private Certificate[] certificateChain;

	private TrustManager[] trustManagers;

	private KeyManager[] keyManagers;

	private SSLContext sslContext;

	private SSLServerSocketFactory socketFactory;

	public SSLCertificateSocketFactory(File certificateFile, File certificatePrivateKeyFile, String certificatePassword) throws FileNotFoundException, IOException, GeneralSecurityException {
		this.certificateFile = certificateFile;
		this.certificatePrivateKeyFile = certificatePrivateKeyFile;
		this.certificatePassword = certificatePassword;
		this.isP7b = certificateFile.getName().endsWith(".p7b");
		load();
	}

	private void load() throws FileNotFoundException, IOException, GeneralSecurityException {
		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(null);
		if(this.isP7b){
			this.certificateChain = loadP7b(certificateFile);
		}
		else {
			this.certificateChain = loadCertificateChain(certificateFile);
		}
		for(int i = 0; i < certificateChain.length; i++) {
			keyStore.setCertificateEntry("certificate" + i, certificateChain[i]);
		}
		keyStore.setKeyEntry("certificateKey", loadCertificateKey(certificatePrivateKeyFile), certificatePassword != null ? certificatePassword.toCharArray() : new char[0], certificateChain);
		this.trustManagers = createTrustManagers(keyStore);
		this.keyManagers = createKeyManagers(keyStore);

		sslContext = SSLContext.getInstance("TLSv1.3");
		sslContext.init(keyManagers, trustManagers, null);
		socketFactory = sslContext.getServerSocketFactory();
	}

	public SSLServerSocket createServerSocket(String host, int port) throws IOException {
		return (SSLServerSocket) socketFactory.createServerSocket(port, 50, InetAddress.getByName(host));
	}

	private X509TrustManager[] createTrustManagers(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
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

	private X509KeyManager[] createKeyManagers(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
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

	private Certificate[] loadCertificateChain(File certFile) throws CertificateException, FileNotFoundException, IOException {
		CertificateFactory f = CertificateFactory.getInstance("X.509");
		try(FileInputStream in = new FileInputStream(certFile)) {
			return f.generateCertificates(in).toArray(Certificate[]::new);
		}
	}

	private PrivateKey loadCertificateKey(File certKeyFile) throws FileNotFoundException, IOException, GeneralSecurityException {
		try(FileInputStream in = new FileInputStream(certKeyFile)) {
			return getPrivateKeyFromString(new String(IOUtils.readAllBytes(in), StandardCharsets.UTF_8));
		}
	}

	private RSAPrivateKey getPrivateKeyFromString(String key) throws IOException, GeneralSecurityException {
		String privateKeyPEM = key;
		privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----", "");
		privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "");
		byte[] encoded = Base64.getDecoder().decode(privateKeyPEM.replace("\n", "").replace("\r", ""));
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
		RSAPrivateKey privKey = (RSAPrivateKey) kf.generatePrivate(keySpec);
		return privKey;
	}
	
	private Certificate[] loadP7b(File certFile) throws GeneralSecurityException, FileNotFoundException, IOException {
		try(FileInputStream in = new FileInputStream(certFile)) {
			return readCertificatesFromPKCS7(getBytesFromP7bString(new String(IOUtils.readAllBytes(in), StandardCharsets.UTF_8)));
		}
	}

	
	private final Certificate[] readCertificatesFromPKCS7(byte[] binaryPKCS7Store) throws GeneralSecurityException, IOException, NullPointerException {
	    try (ByteArrayInputStream bais = new ByteArrayInputStream(binaryPKCS7Store);) {
	        CertificateFactory cf = CertificateFactory.getInstance("X.509");
	        Collection<?> c = cf.generateCertificates(bais);

	        List<Certificate> certList = new ArrayList<Certificate>();

	        if (c.isEmpty()) {
	        	throw new NullPointerException();
	        }
	        else {

	            Iterator<?> i = c.iterator();

	            while (i.hasNext()) {
	                certList.add((Certificate) i.next());
	            }
	        }

	        java.security.cert.Certificate[] certArr = new java.security.cert.Certificate[certList.size()];

	        return certList.toArray(certArr);
	    }
	}
	
	private byte[] getBytesFromP7bString(String key) throws IOException, GeneralSecurityException {
		String privateKeyPEM = key;
		privateKeyPEM = privateKeyPEM.replace("-----BEGIN PKCS7-----", "");
		privateKeyPEM = privateKeyPEM.replace("-----END PKCS7-----", "");
		byte[] encoded = Base64.getDecoder().decode(privateKeyPEM.replace("\n", "").replace("\r", ""));
		return encoded;
	}

}
