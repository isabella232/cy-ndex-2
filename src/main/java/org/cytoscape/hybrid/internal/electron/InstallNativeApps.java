package org.cytoscape.hybrid.internal.electron;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.cytoscape.application.CyApplicationConfiguration;

public class InstallNativeApps {

	private static final String VERSION = "1.0.0";

	// Platform types
	private static final String PLATFORM_WIN = "win";
	private static final String PLATFORM_MAC = "mac";
	private static final String PLATFORM_LINUX = "linux";

	// Archive file names
	private static final String ARCHIVE_MAC = "NDEx-Valet.app.tar.gz";
	private static final String ARCHIVE_LINUX = "NDEx-Valet.tar.gz";
	private static final String ARCHIVE_WIN = "NDEx-Valet.zip";

	private static final String NATIVE_APP_LOCATION = "native";
	private static final String TEMPLATE_NAME = "ndex";
	private static final String VERSION_NAME = "version.txt";

	private static final Map<String, String> COMMANDS = new HashMap<>();
	private static final Map<String, String> ARCHIVE = new HashMap<>();

	static {
		// Commands to execute native Electron App
		COMMANDS.put(PLATFORM_MAC, "NDEx-Valet.app/Contents/MacOS/NDEx-Valet");
		COMMANDS.put(PLATFORM_WIN, "NDEx-Valet/NDEx-Valet.exe");
		COMMANDS.put(PLATFORM_LINUX, "NDEx-Valet/NDEx-Valet");

		ARCHIVE.put(PLATFORM_MAC, ARCHIVE_MAC);
		ARCHIVE.put(PLATFORM_WIN, ARCHIVE_WIN);
		ARCHIVE.put(PLATFORM_LINUX, ARCHIVE_LINUX);
	}

	private final CyApplicationConfiguration appConfig;

	// Native command to execute the Electron app
	private final String command;

	// Type of the platform (Mac, Windows, or Linux)
	private final String platform;

	public InstallNativeApps(final CyApplicationConfiguration appConfig) {
		this.appConfig = appConfig;
		this.platform = detectPlatform();

		final File configLocation = this.appConfig.getConfigurationDirectoryLocation();
		final File electronAppDirectory = new File(configLocation, NATIVE_APP_LOCATION);
		
		// Delete it
		try {
			deleteDirectory(electronAppDirectory.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		electronAppDirectory.mkdir();

		extractNativeApp(electronAppDirectory);

		this.command = getPlatformDependentCommand(electronAppDirectory);
	}

	private final String detectPlatform() {
		final String os = System.getProperty("os.name").toLowerCase();
		if (os.contains(PLATFORM_MAC)) {
			return PLATFORM_MAC;
		} else if (os.contains(PLATFORM_WIN)) {
			return PLATFORM_WIN;
		} else {
			return PLATFORM_LINUX;
		}
	}

	private final void extractNativeApp(final File electronAppDir) {
		final File archiveFile = new File(electronAppDir, ARCHIVE.get(platform));

		switch (platform) {
		case PLATFORM_MAC:
			try {
				extract(archiveFile);
				unzip(archiveFile, electronAppDir);
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case PLATFORM_WIN:

			break;
		case PLATFORM_LINUX:

			break;
		default:
			break;
		}

	}

	// For Mac: Extract with native tar command to avoid broken app.
	private void unzip(File archiveFile, File electronAppDirectory) throws IOException, InterruptedException {

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
			System.out.print((char) c);
		}
	}

	private final void deleteDirectory(final Path directory) throws IOException {
		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private final void extract(File target) throws IOException {
		final URL src = this.getClass().getClassLoader().getResource(TEMPLATE_NAME + "/" + ARCHIVE_MAC);
		InputStream is = src.openStream();
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(target.toString());
			byte[] buf = new byte[2048];
			int r = is.read(buf);
			while (r != -1) {
				fos.write(buf, 0, r);
				r = is.read(buf);
			}
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
	}

	public final void extractPreviewTemplate(File destination) throws IOException {
		// Get the location of web preview template
		final URL source = this.getClass().getClassLoader().getResource(TEMPLATE_NAME);
		final File configLocation = this.appConfig.getConfigurationDirectoryLocation();
		// Unzip resource to this directory in CytoscapeConfig
		if (!destination.exists() || !destination.isDirectory()) {
			unzipTemplate(source, destination);
		} else if (destination.exists()) {
			// Maybe there is an old version
			final File versionFile = new File(destination, VERSION_NAME);
			if (!versionFile.exists()) {
				deleteAll(destination);
				unzipTemplate(source, destination);
			} else {
				// Check version number
				final String contents = Files.lines(Paths.get(versionFile.toURI())).reduce((t, u) -> t + u).get();

				if (!contents.equals(VERSION)) {
					deleteAll(destination);
					unzipTemplate(source, destination);
				} else {
				}
			}
		}
	}

	private final void deleteAll(final File f) {
		if (f.isDirectory()) {
			final File[] files = f.listFiles();
			Arrays.stream(files).forEach(file -> deleteAll(file));
		}
		f.delete();
	}

	public void unzipTemplate(final URL source, final File destDir) throws IOException {

		destDir.mkdir();
		final ZipInputStream zipIn = new ZipInputStream(source.openStream());

		ZipEntry entry = zipIn.getNextEntry();
		while (entry != null) {
			final String filePath = destDir.getPath() + File.separator + entry.getName();
			if (!entry.isDirectory()) {
				unzipEntry(zipIn, filePath);
			} else {
				final File dir = new File(filePath);
				dir.mkdir();
			}
			zipIn.closeEntry();
			entry = zipIn.getNextEntry();
		}
		zipIn.close();
	}

	private final void unzipEntry(final ZipInputStream zis, final String filePath) throws IOException {
		final byte[] buffer = new byte[4096];
		final BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
		int read = 0;
		while ((read = zis.read(buffer)) != -1) {
			bos.write(buffer, 0, read);
		}
		bos.close();
	}

	private final String getPlatformDependentCommand(final File configLocation) {
		final String os = System.getProperty("os.name").toLowerCase();

		File f = null;
		if (os.contains("mac")) {
			// Mac OS X
			f = new File(configLocation, "NDEx-Valet.app/Contents/MacOS/NDEx-Valet");
		} else if (os.contains("win")) {
			// Windows
			f = new File(configLocation, "NDEx-Valet.app/Contents/MacOS/NDEx");
		} else {
			// Linux
		}

		System.out.println("\n\nNDEx Command: " + f.getAbsolutePath());
		return f.getAbsolutePath();
	}

	public void getInstallDirectory() {

	}

	public String getCommand() {
		return this.command;
	}
}
