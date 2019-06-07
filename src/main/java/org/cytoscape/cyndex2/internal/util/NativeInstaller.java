package org.cytoscape.cyndex2.internal.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.cytoscape.cyndex2.internal.util.StringResources.LoadBrowserStage;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeInstaller {
	private final Logger logger = LoggerFactory.getLogger(NativeInstaller.class);

	private static final int BUFFER_SIZE = 2048;

	public static final String JXBROWSER_VERSION = "6.23.1";
	public static final String JXBROWSER_LOCATION = "jxbrowser";

	public static final String PLATFORM_WIN = "win64";
	public static final String PLATFORM_MAC = "mac";
	public static final String PLATFORM_LINUX = "linux64";

	private final String platform;
	private final File installLocation;

	private static final String cdnURL = "https://maven.teamdev.com/repository/products/com/teamdev/jxbrowser/";

	private NativeInstaller(File installLocation) {
		platform = detectPlatform();
		this.installLocation = installLocation;
	}

	private final String detectPlatform() {
		final String os = System.getProperty("os.name").toLowerCase();
		final String arch = System.getProperty("os.arch").toLowerCase();

		logger.info("Target OS = " + os);
		logger.info("Architecture = " + arch);

		if (os.indexOf("mac") >= 0) {
			// Universal binary.
			return PLATFORM_MAC;
		} else if (os.indexOf("win") >= 0) {
			// Win64
			return PLATFORM_WIN;
		} else {
			return PLATFORM_LINUX;
		}
	}

	private final void install(TaskMonitor tm) throws InstallException {
		tm.setTitle("Installing JXBrowser binaries. This should only occur on first run.");
		// Create the directory if it doesn't exist
		if (installLocation.exists()) {
			File jarFile = new File(installLocation, getJarName(this.platform));
			
			if (!jarFile.exists()) {
				//Could be corrupt install or previous version
				logger.info("jxbrowser installation was incomplete or version is out of date. Deleting directory " + installLocation);
				try {
					Files.walk(installLocation.toPath())
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
				} catch (IOException e) {
					throw new InstallException("Unable to delete jxbrowser directory");
				}
				installLocation.mkdir();
			}
		} else {
			installLocation.mkdir();
		}
		
		File jarFile = new File(installLocation, getJarName(this.platform));
		
		try {
			if (!jarFile.exists()) {
				LoadBrowserStage.DOWNLOAD_JAR.updateTaskMonitor(tm);
				String url = getURL();
				logger.info("Downloading JxBrowser JAR file from " + url);
				
				final URL sourceUrl = new URL(url);
				int fileSize = checkSize(url);
				downloadJarFile(sourceUrl, jarFile, fileSize);
			}
			
			if (!jarFile.exists()) {
				throw new InstallException("Unable to download JxBrowser jar file for " + platform);
			}
			LoadBrowserStage.EXTRACT_ZIP.updateTaskMonitor(tm);
			File zipFile = extractZipFile(jarFile, installLocation);
			if (zipFile == null || !zipFile.exists()) {
				throw new InstallException("Unable to extract JxBrowser archive from " + jarFile.getAbsolutePath());
			}
			
			LoadBrowserStage.EXTRACT_BINARY.updateTaskMonitor(tm);
			if (!extractBinaries(zipFile)) {
				throw new InstallException("Unable to extract JxBrowser binaries from archive at " + zipFile.getAbsolutePath());
			}

		} catch (IOException e) {
			e.printStackTrace();
			String message = "Failed to extract JAR to " + installLocation.getAbsolutePath();
			throw new InstallException(message);
		}

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
			Map<String, List<String>> fields = httpConn.getHeaderFields();
			for (Map.Entry<String, List<String>> field : fields.entrySet()) {
				System.err.println(field.getKey());
				for (String value : field.getValue()) {
					System.err.println("\t" + value);
				}
			}
			BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
			for (String line = in.readLine(); line != null; line = in.readLine()) {
				System.err.println(line);
			}
			in.close();
			
			throw new IOException("No file to download at: " + url + " Server replied HTTP code: " + responseCode);
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
		File zipFile = new File(destDir, "chromium-" + platform + ".7z");
		java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile);
		Enumeration<JarEntry> enumEntries = jar.entries();
		while (enumEntries.hasMoreElements()) {
			java.util.jar.JarEntry file = (java.util.jar.JarEntry) enumEntries.nextElement();
			java.io.File f = new java.io.File(destDir + java.io.File.separator + file.getName());

			if (file.getName().startsWith("chromium")) {
				zipFile = f;
			}

			if (f.exists()) {
				continue;
			}
			logger.info("Extracting " + file.getName());

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

	public boolean extractBinaries(File zipFile) throws IOException {
		SevenZFile sevenZFile = new SevenZFile(zipFile);
		SevenZArchiveEntry entry = sevenZFile.getNextEntry();

		while (entry != null) {
			File f = new File(zipFile.getParentFile(), entry.getName());
			if (!f.exists()) {
				logger.info("Extracting " + f.getName());
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
		return true;
	}

	public static String getJarName(String platform) {
		return String.format("jxbrowser-%s-%s.jar", platform, JXBROWSER_VERSION);
	}

	private String getURL() {
		return getURL(this.platform);
	}

	public static String getURL(String platform) {
		final String url = String.format("%sjxbrowser-%s/%s/%s", cdnURL, platform, JXBROWSER_VERSION, getJarName(platform));
		return url;
	}
	
	public static void installJXBrowser(File config, TaskMonitor tm) throws InstallException {
		NativeInstaller ni = new NativeInstaller(config);
		ni.install(tm);
	}
	
	class InstallException extends Exception {

		public InstallException(String string) {
			super(string);
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = -625275343732414469L;
		
	}

}
