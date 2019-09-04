package org.cytoscape.cyndex2.io.cxio.reader;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.cytoscape.cyndex2.internal.util.NativeInstaller;
import org.junit.Test;

public class NativeInstallerTests {

	@Test
	public void getJarNameTest() throws IOException {
		final String jarName = NativeInstaller.getJarName("MYOS");
		assertEquals (jarName, "jxbrowser-MYOS-6.23.1.jar");
	}

	@Test
	public void getURLTest() throws IOException {
		final String url = NativeInstaller.getURL("MYOS");
		assertEquals (url, "http://cyndex.ndexbio.org/jxb/MYOS-6.23.1.jar");
	}
	
	@Test
	public void winDownloadTest() throws IOException {
		final String urlString = NativeInstaller.getURL(NativeInstaller.PLATFORM_WIN);
		final URL url = new URL(urlString);
		final HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		final int responseCode = httpConn.getResponseCode();
		assertEquals (responseCode, HttpURLConnection.HTTP_OK);
	}
	
	@Test
	public void linuxDownloadTest() throws IOException {
		final String urlString = NativeInstaller.getURL(NativeInstaller.PLATFORM_LINUX);
		final URL url = new URL(urlString);
		final HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		final int responseCode = httpConn.getResponseCode();
		assertEquals (responseCode, HttpURLConnection.HTTP_OK);
	}
	
	@Test
	public void macDownloadTest() throws IOException {
		final String urlString = NativeInstaller.getURL(NativeInstaller.PLATFORM_MAC);
		final URL url = new URL(urlString);
		final HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		final int responseCode = httpConn.getResponseCode();
		assertEquals (responseCode, HttpURLConnection.HTTP_OK);
	}
	
}
