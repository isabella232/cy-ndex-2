package org.cytoscape.cyndex2.internal.util;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.commons.io.FileUtils;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.errors.BrowserCreationError;
import org.cytoscape.cyndex2.internal.util.StringResources.LoadBrowserStage;
import org.cytoscape.work.TaskMonitor;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.BrowserContext;
import com.teamdev.jxbrowser.chromium.BrowserContextParams;
import com.teamdev.jxbrowser.chromium.BrowserCore;
import com.teamdev.jxbrowser.chromium.BrowserPreferences;
import com.teamdev.jxbrowser.chromium.BrowserType;
import com.teamdev.jxbrowser.chromium.LoggerProvider;
import com.teamdev.jxbrowser.chromium.PopupContainer;
import com.teamdev.jxbrowser.chromium.PopupHandler;
import com.teamdev.jxbrowser.chromium.PopupParams;
import com.teamdev.jxbrowser.chromium.events.DisposeEvent;
import com.teamdev.jxbrowser.chromium.events.DisposeListener;
import com.teamdev.jxbrowser.chromium.events.LoadAdapter;
import com.teamdev.jxbrowser.chromium.events.LoadEvent;
import com.teamdev.jxbrowser.chromium.internal.ipc.IPC;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;

public class BrowserManager {
	private static Browser browser;
	private static BrowserView browserView;
	private static File jxbrowserDataLocation;

	private static boolean supportedOSAndArchitecture() {
		String os = System.getProperty("os.name");
		if (os.contains("Windows") || os.contains("Mac"))
			return true;
		String arch = System.getProperty("os.arch");
		return arch.endsWith("64");
	}

	public static BrowserView getBrowserView(TaskMonitor tm) throws BrowserCreationError {
		// returns non-null Browser object or an Exception

		if (!supportedOSAndArchitecture()) {
			throw new BrowserCreationError("JxBrowser is not supported on your system.");
		}

		if (browserView == null) {
			Browser b = getJXBrowser(tm);
			browserView = new BrowserView(b);
		}
		return browserView;
	}

	public static void enableLogging() throws IOException {
		LoggerProvider.setLevel(Level.ALL);

		// Uncomment for development port
		// BrowserPreferences.setChromiumSwitches("--remote-debugging-port=9222");
		File dir = new File(jxbrowserDataLocation.getParent(), "log");
		dir.mkdirs();

		// Redirect Browser log messages to jxbrowser-browser.log
		redirectLogMessagesToFile(LoggerProvider.getBrowserLogger(), new File(dir, "browser.log").getAbsolutePath());

		// Redirect IPC log messages to jxbrowser-ipc.log
		redirectLogMessagesToFile(LoggerProvider.getIPCLogger(), new File(dir, "ipc.log").getAbsolutePath());

		// Redirect Chromium Process log messages to jxbrowser-chromium.log
		redirectLogMessagesToFile(LoggerProvider.getChromiumProcessLogger(),
				new File(dir, "chromium.log").getAbsolutePath());
	}

	private static void redirectLogMessagesToFile(Logger logger, String logFilePath) throws IOException {
		FileHandler fileHandler = new FileHandler(logFilePath);
		fileHandler.setFormatter(new SimpleFormatter());

		// Remove default handlers including console handler
		for (Handler handler : logger.getHandlers()) {
			logger.removeHandler(handler);
		}
		logger.addHandler(fileHandler);
	}

	private static boolean parseDebug() {
		String debug = CyActivator.getProperty("jxbrowser.debug");
		return Boolean.parseBoolean(debug);
	}

	public static Browser getJXBrowser(TaskMonitor tm) throws BrowserCreationError {
		tm.setProgress(0.0f);
		if (browser == null) {
			LoadBrowserStage.ENABLE_LOGGING.updateTaskMonitor(tm);
			BrowserPreferences.setChromiumSwitches("--disable-gpu");
			
			if (parseDebug()) {
				BrowserPreferences.setChromiumSwitches("--remote-debugging-port=9222");
				try {
					enableLogging();
					System.setProperty("jxbrowser.ipc.external", "true");
				} catch (IOException e) {
					System.out.println("Failed to load loggers");
				}
			}

			try {

				File f = new File(jxbrowserDataLocation.getParent(), "bin");
				NativeInstaller.installJXBrowser(f, tm);
				System.setProperty("jxbrowser.chromium.dir", f.getAbsolutePath());

				LoadBrowserStage.CREATING_BROWSER.updateTaskMonitor(tm);
				BrowserContextParams params = new BrowserContextParams(jxbrowserDataLocation.getAbsolutePath());
				BrowserContext context = new BrowserContext(params);
				browser = new Browser(BrowserType.LIGHTWEIGHT, context);

				if (browser == null) {
					throw new BrowserCreationError("Browser failed to initialize.");
				}

				LoadBrowserStage.BROWSER_SETUP.updateTaskMonitor(tm);
				// Enable local storage and popups
				BrowserPreferences preferences = browser.getPreferences();
				preferences.setLocalStorageEnabled(true);
				browser.setPreferences(preferences);
				browser.addLoadListener(new LoadAdapter() {
					@Override
					public void onDocumentLoadedInMainFrame(LoadEvent event) {
						Browser browserLocal = event.getBrowser();
						browserLocal.executeJavaScript("localStorage");
					}
				});

				browser.setPopupHandler(new CustomPopupHandler());
			} catch (Exception e) {
				e.printStackTrace();
				browser = null;
				throw new BrowserCreationError(e.getMessage());
			}

		}

		return browser;

	}

	private static class CustomPopupHandler implements PopupHandler {
		@Override
		public PopupContainer handlePopup(PopupParams params) {
			return new PopupContainer() {
				@Override
				public void insertBrowser(final Browser browserParam, final Rectangle initialBounds) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							BrowserView browserViewLocal = new BrowserView(browserParam);
							browserViewLocal.setPreferredSize(initialBounds.getSize());

							final JFrame frame = new JFrame("Popup");
							frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
							frame.add(browserViewLocal, BorderLayout.CENTER);
							frame.pack();
							frame.setLocation(initialBounds.getLocation());
							frame.setVisible(true);

							browserParam.addDisposeListener(new DisposeListener<Browser>() {
								@Override
								public void onDisposed(DisposeEvent<Browser> event) {
									frame.setVisible(false);
								}
							});
						}
					});
				}
			};
		}
	}

	public static File getDataDirectory() {
		return jxbrowserDataLocation;
	}

	public static void setDataDirectory(File directory) {
		// JXBrowser configuration
		jxbrowserDataLocation = directory;
		if (!jxbrowserDataLocation.exists())
			try {
				jxbrowserDataLocation.mkdirs();
			} catch (SecurityException e) {
				ExternalAppManager.setLoadFailed(
						"Failed to create JXBrowser directory in CytoscapeConfiguration: " + e.getMessage());
			}
		System.setProperty(BrowserPreferences.TEMP_DIR_PROPERTY, jxbrowserDataLocation.getAbsolutePath());
		System.setProperty(BrowserPreferences.USER_AGENT_PROPERTY, jxbrowserDataLocation.getAbsolutePath());

	}

	public static void clearCache() {
		if (browser != null)
			browser.getCacheStorage().clearCache();
		try {
			if (jxbrowserDataLocation.exists()) {
				File cacheDir = new File(jxbrowserDataLocation.getAbsolutePath(), "Cache");
				FileUtils.deleteDirectory(cacheDir);
			}
		} catch (IOException e) {

		}
	}

	public static void shutdown() {
		if (browser != null) {
			for (Browser b : IPC.getBrowsers()) {
				b.dispose();
			}
			try {
				BrowserCore.shutdown();
			} catch (Exception e) {
				// IGNORE
			}
		}
	}
}
