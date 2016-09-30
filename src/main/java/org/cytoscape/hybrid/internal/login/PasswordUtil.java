package org.cytoscape.hybrid.internal.login;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Base64.Encoder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class PasswordUtil {

	private static final String ALGORITHM = "DES";

	private final Cipher cipher;
	private final Encoder encoder;

	public PasswordUtil() {
		this.encoder = Base64.getEncoder();
		
		try {
			this.cipher = Cipher.getInstance(ALGORITHM);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Could not create instance of password util", e);
		}
	}

	public final void storeKey(final SecretKey key, final File keyFile) throws IOException {
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(keyFile));
		try {
			out.writeObject(key);
		} finally {
			out.close();
		}
	}

	public final SecretKey loadKey(final File keyFile) throws IOException, ClassNotFoundException {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(keyFile));

		try {
			SecretKey key = (SecretKey) in.readObject();
			return key;
		} finally {
			in.close();
		}
	}

	public final SecretKey generateKey() {
		KeyGenerator keyGen = null;
		try {
			keyGen = KeyGenerator.getInstance(ALGORITHM);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		keyGen.init(56);
		return keyGen.generateKey();
	}

	/**
	 * Encrypt original text (result is encoded in Base64)
	 * 
	 * @param original
	 * @param key
	 * 
	 * @return
	 * @throws GeneralSecurityException
	 * @throws IOException 
	 */
	public String encode(final String original, final SecretKey key) throws GeneralSecurityException, IOException {
		cipher.init(Cipher.ENCRYPT_MODE, key);
		final byte[] text = original.getBytes("UTF-8");
		final byte[] encrypted = cipher.doFinal(text);
		return encoder.encodeToString(encrypted);
	}

	public String decode(final String base64encrypted, final SecretKey key)
			throws ClassNotFoundException, IOException, GeneralSecurityException {
		cipher.init(Cipher.DECRYPT_MODE, key);
		
		final byte[] decoded = Base64.getDecoder().decode(base64encrypted);
		final byte[] decrypted = cipher.doFinal(decoded);
		return new String(decrypted);
	}
}
