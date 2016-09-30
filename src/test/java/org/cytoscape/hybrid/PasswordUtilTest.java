package org.cytoscape.hybrid;

import static org.junit.Assert.*;

import org.cytoscape.hybrid.internal.login.PasswordUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;


public class PasswordUtilTest {
	
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	@Test
	public void testCypher() throws Exception {
		final File keyFile = tempFolder.newFile("ndex-key");
		System.out.println(keyFile.getAbsolutePath());
		
		final PasswordUtil util = new PasswordUtil();
		final SecretKey key = util.generateKey();
	
		// Simple encoding and decoding
		final String originalText = "This is a test";
		final String encoded = util.encode(originalText, key);
		
		System.out.println("Encoded text: " + encoded);
		
		final String decoded = util.decode(encoded, key);
		
		System.out.println("Decoded text: " + decoded);
		assertEquals(originalText, decoded);
		
		// Test stored key
		util.storeKey(key, keyFile);
		final SecretKey restoredKey = util.loadKey(keyFile);
		final String decoded2 = util.decode(encoded, restoredKey);	
		assertEquals(originalText, decoded2);
	
		
		// Try bad key
		final SecretKey badKey = util.generateKey();
		try {
			final String decoded3 = util.decode(encoded, badKey);
		} catch(Exception ex) {
			ex.printStackTrace();
			assertEquals(BadPaddingException.class, ex.getClass());
		}
	}

}
