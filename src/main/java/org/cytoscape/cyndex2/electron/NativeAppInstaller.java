package org.cytoscape.cyndex2.electron;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.cyndex2.internal.util.NetworkConnectionTester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NativeAppInstaller {
	
	private static final Logger logger = LoggerFactory.getLogger(NativeAppInstaller.class);
	
	private static final String APP_URL_PROP_NAME = "cyndex.";
	public static final String LOAD_URL_PROP_NAME = APP_URL_PROP_NAME + "url.load";
	public static final String SAVE_URL_PROP_NAME = APP_URL_PROP_NAME + "url.save";
	
	public static final String NATIVE_APP_LOCATION = "ndex-electron";
	
	// Marker file prefix.  "ndex-installed-2.x.x.txt" will be the actual file name.
	public static final String INSTALL_MAKER_FILE_NAME = "ndex-installed";

	private static final int BUFFER_SIZE = 2048;
	
	// Platform types
	private static final String PLATFORM_WIN = "win";
	private static final String PLATFORM_MAC = "mac";
	private static final String PLATFORM_LINUX = "linux";

	private static final String PLATFORM_WIN_32 = "win32";
	private static final String PLATFORM_LINUX_32 = "linux32";
	
	// Archive file names
	private static final String ARCHIVE_MAC = "CyNDEx-2-mac.tar.gz";
	private static final String ARCHIVE_LINUX = "CyNDEx-2-linux64.tar.gz";
	private static final String ARCHIVE_WIN = "CyNDEx-2-win64.zip";
	
	// Legacy versions
	private static final String ARCHIVE_LINUX_32 = "CyNDEx-2-linux32.tar.gz";
	private static final String ARCHIVE_WIN_32 = "CyNDEx-2-win32.zip";

	private static final Map<String, String> COMMANDS = new HashMap<>();
	private static final Map<String, String> ARCHIVE = new HashMap<>();

	static {
		// Commands to execute native Electron App
		COMMANDS.put(PLATFORM_MAC, "CyNDEx-2.app/Contents/MacOS/CyNDEx-2");
		COMMANDS.put(PLATFORM_WIN, "CyNDEx-2-win64/CyNDEx-2.exe");
		COMMANDS.put(PLATFORM_LINUX, "CyNDEx-2-linux-x64/CyNDEx-2");
		COMMANDS.put(PLATFORM_WIN_32, "CyNDEx-2-win64/CyNDEx-2.exe");
		COMMANDS.put(PLATFORM_LINUX_32, "CyNDEx-2-linux-x64/CyNDEx-2");

		ARCHIVE.put(PLATFORM_MAC, ARCHIVE_MAC);
		ARCHIVE.put(PLATFORM_WIN, ARCHIVE_WIN);
		ARCHIVE.put(PLATFORM_LINUX, ARCHIVE_LINUX);
		ARCHIVE.put(PLATFORM_WIN_32, ARCHIVE_WIN_32);
		ARCHIVE.put(PLATFORM_LINUX_32, ARCHIVE_LINUX_32);
	}

	private final CyApplicationConfiguration appConfig;

	// Native command to execute the Electron app
	private final String command;

	// Type of the platform (Mac, Windows, or Linux)
	private final String platform;
	
	private final JProgressBar bar;
	
	private File electronAppDir;
	private JPanel uiPanel;
	
	private JPanel progressPanel;
	private JToolBar toolBar;
	private CySwingApplication desktop;
	
	private final String bundleVersion;
	
	private final String cdnHost;
	private final String cdnUrl;

	// Currently, this is an empty file just for checking installation.
	// Maybe used as real config file in future.
	private File markerFile;

	public NativeAppInstaller(final CyApplicationConfiguration appConfig, JProgressBar bar, 
			JPanel progressPanel, JToolBar toolBar, CySwingApplication desktop, final String bundleVersion,
			final String cdnHost, final String cdnUrl) {
		this.appConfig = appConfig;
		this.bar = bar;
		this.progressPanel = progressPanel;
		this.toolBar = toolBar;
		this.desktop = desktop;
		
		this.bundleVersion = bundleVersion;
		this.cdnUrl = cdnUrl;
		this.cdnHost = cdnHost;
		
		this.platform = detectPlatform();

		final File configLocation = this.appConfig.getConfigurationDirectoryLocation();
		final String appDir = NATIVE_APP_LOCATION + "-" + bundleVersion;
		this.electronAppDir = new File(configLocation, appDir);
		this.markerFile = new File(configLocation, INSTALL_MAKER_FILE_NAME + "-" + bundleVersion + ".txt");
	
		checkVersion(configLocation.toString(), electronAppDir.toString());

		this.command = getPlatformDependentCommand(electronAppDir);
	}
	
	
	private final Boolean isInstalled(final File electronAppDirectory) {
		// 1. Check status of the install from file
		
		// 1. no Electron directory --> force to install everything
		if(!electronAppDirectory.exists()) {
			return false;
		}
		
		// 2. Directory exists.  Check it's valid or not
		
		// 2.1. No marker file
		if(!markerFile.exists()) {
			return false;
		} else {
			// TODO: check version number here.
			final String markerFileName = markerFile.getName();
			final String regex = "[0-9]+\\.[0-9]+\\.[0-9]+";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(markerFileName);
			if(m.find()) {
				final String match = m.group();
				if(match.equals(bundleVersion)) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
	}
	
	
	private final void install(final String configDir, final File electronAppDirectory, final String hostName) {
		if(isInstalled(electronAppDirectory)) {
			// Directory already exists.  Simply add the NDEx Search box.
			toolBar.add(uiPanel);
		} else {
			// Check network connection first.
			final boolean isNetworkAvailable = NetworkConnectionTester.isReacheable(hostName);

			if(!isNetworkAvailable) {
				final String errorMessage = "Could not install CyNDEx-2 app due to a network problem.  "
						+ "Please check your network connection and try again later.";
				logger.error(errorMessage);
				
				SwingUtilities.invokeLater(new Runnable() {
					
					@Override
					public void run() {	
						JOptionPane.showMessageDialog(
								desktop.getJFrame(),
								errorMessage,
								"Failed to Install CyNDEx-2",
								JOptionPane.ERROR_MESSAGE);
					}
				});
				throw new RuntimeException("Could not connect to the external server.");
			}
			
			// Clean up unnecessary old files.

			deleteMarkerFiles(configDir);
			
			if(electronAppDirectory.exists()) {
				deleteAll(electronAppDirectory);
			}
			
			electronAppDirectory.mkdir();
			toolBar.add(progressPanel);
			
			try {
				extractNativeApp(electronAppDirectory);
				toolBar.remove(progressPanel);
				toolBar.add(uiPanel);
				uiPanel.updateUI();
				uiPanel.repaint();
				desktop.getJFrame().repaint();
				toolBar.repaint();
				toolBar.updateUI();
				
				markerFile.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException("Failed to install native app", e);
			}
		}
	}
	
	private final void deleteMarkerFiles(final String configDir) {
		final File f = new File(configDir);
		final File[] markerFiles = f.listFiles(new FilenameFilter() {
		    public boolean accept(File dir, String name) {
		        return name.startsWith(INSTALL_MAKER_FILE_NAME) && name.endsWith(".txt");
		    }
		});
		
		Arrays.asList(markerFiles).stream().forEach(file->file.delete());
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
                    fileName = disposition.substring(index + 10,
                            disposition.length() - 1);
                }
            } else {
                // extracts file name from URL
                fileName = fileURL.substring(fileURL.lastIndexOf("/") + 1,
                        fileURL.length());
            }
 
            // opens input stream from the HTTP connection
            httpConn.disconnect();
            return contentLength;
 
        } else {
            throw new IOException(
                    "No file to download. Server replied HTTP code: "
                            + responseCode);
        }
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
			
			if(arch.equals("x86")) {
				return PLATFORM_WIN;
			} else {
				return PLATFORM_WIN;
			}
		} else {
			if(arch.equals("amd64")) {
				return PLATFORM_LINUX;
			} else {
				return PLATFORM_LINUX_32;
			}
		}
	}
	
	private final void checkVersion(final String cyConfigDir, final String current) {
		final Path path = FileSystems.getDefault().getPath(cyConfigDir);
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path, NATIVE_APP_LOCATION + "*")) {
		    for(final Path file : directoryStream) {
		        if(!file.toString().equals(current)) {
		        		deleteDirectory(file);
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
				int fileSize = checkSize(cdnUrl + ARCHIVE_MAC);
				final URL sourceUrl = new URL(cdnUrl + ARCHIVE_MAC);
				extract(sourceUrl, archiveFile, fileSize);
				unzipMac(archiveFile, electronAppDir);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("Failed to extract", e);
			}
			break;
		case PLATFORM_WIN:
			final URL sourceUrl = new URL(cdnUrl + ARCHIVE_WIN);
			extractPreviewTemplate(sourceUrl, electronAppDir);
			break;
		case PLATFORM_LINUX:
			final URL linuxSourceUrl = new URL(cdnUrl + ARCHIVE_LINUX);
			int fileSize = checkSize(cdnUrl + ARCHIVE_LINUX);
			extract(linuxSourceUrl, archiveFile, fileSize);
			try {
				unzip(archiveFile, electronAppDir);
			} catch (InterruptedException e) {
				e.printStackTrace();
				logger.error("Failed to extract", e);
			}
			break;
		default:
			break;
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
				while(is.read() >= 0);
			} finally {
				is.close();
			}

			p.waitFor();

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Failed to extract Electron file.", e);
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
				if(idx % 400 == 0) {
					Double progress = (finished/size)* 100;
					if(progress>=100) {
						progress = 100.0;
					}
					this.bar.setValue(progress.intValue());
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
			logger.error("Failed to download Electron binary");
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
