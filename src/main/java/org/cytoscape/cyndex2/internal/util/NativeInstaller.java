package org.cytoscape.cyndex2.internal.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeInstaller {
	private final Logger logger = LoggerFactory.getLogger(NativeInstaller.class);

	private static final int BUFFER_SIZE = 2048;

	public static final String JXBROWSER_VERSION = "6.20";
	public static final String JXBROWSER_LOCATION = "jxbrowser";

	private static final String PLATFORM_WIN = "win";
	private static final String PLATFORM_MAC = "mac";
	private static final String PLATFORM_LINUX_32 = "linux32";
	private static final String PLATFORM_LINUX_64 = "linux64";

	private final String platform;
	private final File installLocation;

	private final String cdnURL = "http://maven.teamdev.com/repository/products/com/teamdev/jxbrowser/";

	private NativeInstaller(File installLocation) {
		platform = detectPlatform();
		this.installLocation = installLocation;
	}

	private final String detectPlatform() {
		final String os = System.getProperty("os.name").toLowerCase();
		final String arch = System.getProperty("os.arch").toLowerCase();

		logger.info("Target OS = " + os);
		logger.info("Architecture = " + arch);

		if (os.contains(PLATFORM_MAC)) {
			// Universal binary.
			return PLATFORM_MAC;
		} else if (os.contains(PLATFORM_WIN)) {

			if (arch.equals("x86")) {
				return PLATFORM_WIN;
			} else {
				return PLATFORM_WIN;
			}
		} else {
			if (arch.equals("amd64")) {
				return PLATFORM_LINUX_64;
			} else {
				return PLATFORM_LINUX_32;
			}
		}
	}

	public void executeInstaller() {
		// Extract binary from archive
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			install();
		});
	}

	private final File install() {

		// Create the directory if it doesn't exist
		if (!installLocation.exists()) {
			installLocation.mkdir();
		}

		File jarFile = new File(installLocation, getJarName());

		try {
			if (!jarFile.exists()) {
				String url = getURL();
				final URL sourceUrl = new URL(url);
				int fileSize = checkSize(url);
				downloadJarFile(sourceUrl, jarFile, fileSize);
			}

			File zipFile = extractZipFile(jarFile, installLocation);

			extractBinaries(zipFile);

		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Failed to extract JAR to " + installLocation.getAbsolutePath());
			System.out.println("Failed to extract JAR to " + installLocation.getAbsolutePath() + ": " + e.getMessage());
			return null;
		}

		return jarFile;
	}

	private final int checkSize(String fileURL) throws IOException {

		URL url = new URL(fileURL);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		int responseCode = httpConn.getResponseCode();

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {
			String disposition = httpConn.getHeaderField("Content-Disposition");
			//String contentType = httpConn.getContentType();
			int contentLength = httpConn.getContentLength();

			String fileName = null;
			if (disposition != null) {
				// extracts file name from header field
				int index = disposition.indexOf("filename=");
				if (index > 0) {
					fileName = disposition.substring(index + 10, disposition.length() - 1);
				}
			} else {
				// extracts file name from URL
				fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1, fileURL.length());
			}

			// opens input stream from the HTTP connection
			httpConn.disconnect();
			return contentLength;

		} else {
			throw new IOException("No file to download. Server replied HTTP code: " + responseCode);
		}
	}

	private final void downloadJarFile(final URL src, File target, final int size) throws IOException {
		InputStream is = src.openStream();
		FileOutputStream fos = null;
		double finished = 0.0;

		int idx = 0;

		try {
			fos = new FileOutputStream(target.toString());
			byte[] buf = new byte[BUFFER_SIZE];
			int r = is.read(buf);
			while (r != -1) {
				fos.write(buf, 0, r);
				r = is.read(buf);
				finished += r;
				if ((int) (finished / size * 100) > idx) {
					idx = (int) (finished / size * 100);
					logger.info(target.getName() + " download " + idx + "% complete");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Failed to download JXBrowser binary");
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	private final File extractZipFile(File jarFile, File destDir) throws IOException {
		File zipFile = null;
		java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile);
		Enumeration<JarEntry> enumEntries = jar.entries();
		while (enumEntries.hasMoreElements()) {
			java.util.jar.JarEntry file = (java.util.jar.JarEntry) enumEntries.nextElement();
			java.io.File f = new java.io.File(destDir + java.io.File.separator + file.getName());

			if (file.getName().matches("chromium-.*\\.7z")) {
				zipFile = f;
			}

			if (f.exists()) {
				break;
			}

			if (file.isDirectory()) { // if its a directory, create it
				f.mkdir();
				continue;
			}
			java.io.InputStream is = jar.getInputStream(file); // get the input stream
			java.io.FileOutputStream fos = new java.io.FileOutputStream(f);

			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				fos.write(buffer, 0, bytesRead);
			}

			fos.close();
			is.close();

		}
		jar.close();
		return zipFile;
	}

	public void extractBinaries(File zipFile) throws IOException {
		SevenZFile sevenZFile = new SevenZFile(zipFile);
		SevenZArchiveEntry entry = sevenZFile.getNextEntry();

		while (entry != null) {
			File f = new File(zipFile.getParentFile(), entry.getName());
			if (!f.exists()) {
				if (entry.isDirectory()) {
					f.mkdirs();
					entry = sevenZFile.getNextEntry();
					continue;
				}

				byte[] content = new byte[(int) entry.getSize()];
				sevenZFile.read(content);

				FileOutputStream fos = new FileOutputStream(f);
				fos.write(content);
				fos.close();
			}
			f.setExecutable(true);
			entry = sevenZFile.getNextEntry();
		}
		sevenZFile.close();
	}

	public final void extractPreviewTemplate(final URL source, final File destination) throws IOException {
		if (!destination.exists() || !destination.isDirectory()) {
			// unzipTemplate(source, destination);
		} else {
			// unzipTemplate(source, destination);
		}
	}

	private String getJarName() {
		return String.format("jxbrowser-%s-%s.jar", platform, JXBROWSER_VERSION);
	}

	private String getURL() {
		String url = String.format("%s/jxbrowser-%s/%s/%s", cdnURL, platform, JXBROWSER_VERSION, getJarName());
		return url;
	}

	public static void installJXBrowser(File config) {
		NativeInstaller ni = new NativeInstaller(config);
		ni.install();
	}

	public static void main(String[] args) {
		File f = new File("/Users/bsettle/Desktop/jxbrowser");
		NativeInstaller.installJXBrowser(f);
	}

}
