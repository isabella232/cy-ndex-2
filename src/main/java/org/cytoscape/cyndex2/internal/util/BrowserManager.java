package org.cytoscape.cyndex2.internal.util;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.commons.io.FileUtils;
import org.cytoscape.cyndex2.errors.BrowserCreationError;

import com.teamdev.jxbrowser.chromium.Browser;
import com.teamdev.jxbrowser.chromium.BrowserContext;
import com.teamdev.jxbrowser.chromium.BrowserContextParams;
import com.teamdev.jxbrowser.chromium.BrowserPreferences;
import com.teamdev.jxbrowser.chromium.BrowserType;
import com.teamdev.jxbrowser.chromium.PopupContainer;
import com.teamdev.jxbrowser.chromium.PopupHandler;
import com.teamdev.jxbrowser.chromium.PopupParams;
import com.teamdev.jxbrowser.chromium.events.DisposeEvent;
import com.teamdev.jxbrowser.chromium.events.DisposeListener;
import com.teamdev.jxbrowser.chromium.events.LoadAdapter;
import com.teamdev.jxbrowser.chromium.events.LoadEvent;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;

public class BrowserManager {
	private static Browser browser;
	private static BrowserView browserView;
	private static File jxbrowserConfigLocation;

	private static boolean supportedOSAndArchitecture() {
		String os = System.getProperty("os.name");
		if (os.contains("Windows") || os.contains("Mac"))
			return true;
		String arch = System.getProperty("os.arch");
		return arch.endsWith("64");
	}

	public static BrowserView getBrowserView() throws BrowserCreationError {
		// returns non-null Browser object or an Exception

		if (!supportedOSAndArchitecture()) {
			throw new BrowserCreationError("JxBrowser is not supported on your system.");
		}

		if (browserView == null) {
			Browser b = getJXBrowser();
			browserView = new BrowserView(b);
		}
		return browserView;
	}

	public static Browser getJXBrowser() throws BrowserCreationError {
		if (browser == null) {

			// Uncomment for development port
			BrowserPreferences.setChromiumSwitches("--remote-debugging-port=9222", "--ipc-connection-timeout=2");
			// BrowserPreferences.setChromiumSwitches("--ipc-connection-timeout=2");

			// Create the binary in the CytoscapeConfig
			System.setProperty("jxbrowser.chromium.dir", jxbrowserConfigLocation.getAbsolutePath());

			try {

				BrowserContextParams params = new BrowserContextParams(jxbrowserConfigLocation.getAbsolutePath());
				BrowserContext context = new BrowserContext(params);
				browser = new Browser(BrowserType.LIGHTWEIGHT, context);
				
				if (browser == null) {
					throw new BrowserCreationError("Browser failed to initialize.");
				}
				
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
							frame.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
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

	public static void setConfigurationDirectory(File directory) {
		// JXBrowser configuration
		jxbrowserConfigLocation = directory;
		if (!jxbrowserConfigLocation.exists())
			try {
				jxbrowserConfigLocation.mkdir();
			} catch (SecurityException e) {
				ExternalAppManager.setLoadFailed(
						"Failed to create JXBrowser directory in CytoscapeConfiguration: " + e.getMessage());
			}
		BrowserPreferences.setChromiumDir(jxbrowserConfigLocation.getAbsolutePath());
		System.setProperty(BrowserPreferences.TEMP_DIR_PROPERTY, jxbrowserConfigLocation.getAbsolutePath());
		System.setProperty(BrowserPreferences.CHROMIUM_DIR_PROPERTY, jxbrowserConfigLocation.getAbsolutePath());
		System.setProperty(BrowserPreferences.USER_AGENT_PROPERTY, jxbrowserConfigLocation.getAbsolutePath());

	}

	public static void clearCache() {
		if (browser != null)
			browser.getCacheStorage().clearCache();
		try {
			if (jxbrowserConfigLocation.exists()) {
				File cacheDir = new File(jxbrowserConfigLocation.getAbsolutePath(), "Cache");
				FileUtils.deleteDirectory(cacheDir);
			}
		} catch (IOException e) {

		}
	}

}
