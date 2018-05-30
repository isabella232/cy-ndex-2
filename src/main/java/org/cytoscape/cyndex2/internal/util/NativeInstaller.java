package org.cytoscape.cyndex2.internal.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.teamdev.jxbrowser.chromium.Browser;

public class NativeInstaller {
	private final Logger logger = LoggerFactory.getLogger(NativeInstaller.class);

	private static final int BUFFER_SIZE = 2048;

	public static final String JXBROWSER_VERSION = "6.17";
	public static final String JXBROWSER_LOCATION = "jxbrowser";

	private static final String PLATFORM_WIN = "win";
	private static final String PLATFORM_MAC = "mac";
	private static final String PLATFORM_LINUX_32 = "linux32";
	private static final String PLATFORM_LINUX_64 = "linux64";

	private final String platform;
	private final File installLocation;

	private final String cdnURL = "http://maven.teamdev.com/repository/products/com/teamdev/jxbrowser/";

	public NativeInstaller(File installLocation) {
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

	private boolean isInstalled(File f) {
		if (f.exists()){
			return true;
		}
		return false;
	}

	private final void install() {
		if (!installLocation.exists()) {
			installLocation.mkdir();
		}
		File jarFile = new File(installLocation, getJarName());
		if (!isInstalled(jarFile)) {
			try {
				extractNativeApp(jarFile);
				
			} catch (IOException e) {
				logger.error("Failed to extract JAR to " + jarFile.getAbsolutePath());
				return;
			}

		}
		logger.info("Extracted JXBrowser jar to " + jarFile.getAbsolutePath());
//		addJarToClasspath(jarFile);
	}
	
	private final int checkSize(String fileURL) throws IOException {

		URL url = new URL(fileURL);
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
		int responseCode = httpConn.getResponseCode();

		// always check HTTP response code first
		if (responseCode == HttpURLConnection.HTTP_OK) {
			String disposition = httpConn.getHeaderField("Content-Disposition");
			String contentType = httpConn.getContentType();
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

	private final void extractNativeApp(File jarFile) throws IOException {
		String url = getURL();

		switch (platform) {
		case PLATFORM_MAC:
			try {
				int fileSize = checkSize(url);
				final URL sourceUrl = new URL(url);
				extract(sourceUrl, jarFile, fileSize);
				unzipMac(jarFile, installLocation);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Failed to extract", e);
			}
			break;
		case PLATFORM_WIN:
			final URL sourceUrl = new URL(url);
			extractPreviewTemplate(sourceUrl, installLocation);
			break;
		case PLATFORM_LINUX_32:
		case PLATFORM_LINUX_64:
			final URL linuxSourceUrl = new URL(url);
			int fileSize = checkSize(url);
			extract(linuxSourceUrl, jarFile, fileSize);
			try {
				unzip(jarFile, installLocation);
			} catch (InterruptedException e) {
				e.printStackTrace();
				logger.error("Failed to extract", e);
			}
			break;
		default:
			break;
		}

	}
	
	private final void extract(final URL src, File target, final int size) throws IOException {
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
				finished += BUFFER_SIZE;
				idx++;
				if (idx % 400 == 0) {
					Double progress = (finished / size) * 100;
					if (progress >= 100) {
						progress = 100.0;
					}
					logger.info("Progress " + progress.intValue());
					// this.bar.setValue(progress.intValue());
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

	public final void extractPreviewTemplate(final URL source, final File destination) throws IOException {
		if (!destination.exists() || !destination.isDirectory()) {
//			unzipTemplate(source, destination);
		} else {
//			unzipTemplate(source, destination);
		}
	}
	
	// For Mac: Extract with native tar command to avoid broken app.
	private void unzipMac(File archiveFile, File electronAppDirectory) throws IOException, InterruptedException {

		ProcessBuilder pb = new ProcessBuilder("tar", "zxvf", archiveFile.toString(), "-C",
				electronAppDirectory.toString());
		Process p = pb.start();
		InputStream stream = p.getErrorStream();
		while (true) {
			int c = stream.read();
			if (c == -1) {
				stream.close();
				break;
			}
		}
	}

	// For Linux: Extract with native tar command to avoid broken app.
	private void unzip(File archiveFile, File electronAppDirectory) throws IOException, InterruptedException {

		try {
			ProcessBuilder pb = new ProcessBuilder("tar", "zxvf", archiveFile.toString(), "-C",
					electronAppDirectory.toString());
			Process p = pb.start();
			InputStream is = p.getInputStream();
			try {
				while (is.read() >= 0)
					;
			} finally {
				is.close();
			}

			p.waitFor();

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Failed to extract Electron file.", e);
		}
	}
	
	public boolean addJarToClasspath(File jarFile) 
	{ 
	   try 
	   { 
	       URL url = jarFile.toURI().toURL();
	       URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader(); 
	       Class<?> urlClass = URLClassLoader.class;
	       Method method = urlClass.getDeclaredMethod("addURL", new Class<?>[] { URL.class }); 
	       method.setAccessible(true);
	       method.invoke(urlClassLoader, new Object[] { url });
	       
	   } 
	   catch (Throwable t) 
	   {
		   logger.error("Failed to install JXBrowser jar at " + jarFile.toString() + "\n" + t.getMessage());
		   return false;
	   } 
	   return true;
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
		ni.install(); //executeInstaller();		
	}
	
	public static void main(String[] args) {
		NativeInstaller ni = new NativeInstaller(new File("/Users/bsettle/Desktop/jxbrowser"));
		System.setProperty("jxbrowser.chromium.dir", ni.installLocation.getAbsolutePath());
		ni.executeInstaller();
		Browser b = new Browser();
		System.out.println(b.getURL());
	}
}
