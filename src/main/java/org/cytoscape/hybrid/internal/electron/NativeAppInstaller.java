package org.cytoscape.hybrid.internal.electron;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.cytoscape.application.CyApplicationConfiguration;

public final class NativeAppInstaller {
	private static final String VERSION = "1.2.3";
	private static final String NATIVE_APP_LOCATION = "ndex-electron";
	private static final String APP_DIR = NATIVE_APP_LOCATION + "-" + VERSION;

	// Platform types
	private static final String PLATFORM_WIN = "win";
	private static final String PLATFORM_MAC = "mac";
	private static final String PLATFORM_LINUX = "linux";

	// Archive file names
	private static final String ARCHIVE_MAC = "NDEx-Valet-mac.tar.gz";
	private static final String ARCHIVE_LINUX = "NDEx-Valet-linux.tar.gz";
	private static final String ARCHIVE_WIN = "NDEx-Valet-win64.zip";

	private static final String TEMPLATE_NAME = "ndex";

	private static final Map<String, String> COMMANDS = new HashMap<>();
	private static final Map<String, String> ARCHIVE = new HashMap<>();

	static {
		// Commands to execute native Electron App
		COMMANDS.put(PLATFORM_MAC, "NDEx-Valet.app/Contents/MacOS/NDEx-Valet");
		COMMANDS.put(PLATFORM_WIN, "NDEx-Valet-win64/NDEx-Valet.exe");
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

	public NativeAppInstaller(final CyApplicationConfiguration appConfig) {
		this.appConfig = appConfig;
		this.platform = detectPlatform();

		final File configLocation = this.appConfig.getConfigurationDirectoryLocation();
		final File electronAppDirectory = new File(configLocation, APP_DIR);
	
		checkVersion(configLocation.toString(), electronAppDirectory.toString());
		
		if(!electronAppDirectory.exists()) {
			electronAppDirectory.mkdir();
			try {
				extractNativeApp(electronAppDirectory);
			} catch (IOException e) {
				throw new RuntimeException("Failed to install native app", e);
			}
		}

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
	
	private final void checkVersion(final String cyConfigDir, final String current) {
		final Path path = FileSystems.getDefault().getPath(cyConfigDir);
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path, NATIVE_APP_LOCATION + "*")) {
		    for(final Path file : directoryStream) {
		        if(!file.toString().equals(current)) {
		        		deleteDirectory(file);
		        		System.out.println("Old files deleted: " + file.toString());
		        }
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}

	private final void extractNativeApp(final File electronAppDir) throws IOException {
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
			final URL source = this.getClass().getClassLoader().getResource(TEMPLATE_NAME + "/" + ARCHIVE_WIN);
			extractPreviewTemplate(source, electronAppDir);
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

	public final void extractPreviewTemplate(final URL source, final File destination) throws IOException {
		if (!destination.exists() || !destination.isDirectory()) {
			unzipTemplate(source, destination);
		} else {
			unzipTemplate(source, destination);
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
		final File executable = new File(configLocation, COMMANDS.get(platform));
		return executable.getAbsolutePath();
	}


	public String getCommand() {
		return this.command;
	}
}
