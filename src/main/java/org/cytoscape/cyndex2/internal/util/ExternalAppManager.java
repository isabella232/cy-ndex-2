package org.cytoscape.cyndex2.internal.util;

import java.io.File;

import org.cytoscape.cyndex2.internal.CyActivator;

import com.teamdev.jxbrowser.chromium.BrowserContext;
import com.teamdev.jxbrowser.chromium.internal.ipc.IPCException;
import com.teamdev.jxbrowser.chromium.swing.BrowserView;

public class ExternalAppManager {

	public static final String APP_NAME_LOAD = "choose";
	public static final String APP_NAME_SAVE = "save";

	private BrowserView browserView;
	private boolean loadFailed;
	private String query;
	private String appName;

	public boolean dylibExists() {
		File tempDir = new File(BrowserContext.defaultContext().getDataDir(), "Temp");
		return (tempDir.exists() && tempDir.listFiles().length > 0);
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getQuery() {
		return query;
	}

	public boolean loadFailed() {
		return loadFailed || (browserView == null && dylibExists());
	}

	public BrowserView getBrowserView() {
		if (!loadFailed() && browserView == null)
			try {
				browserView = new BrowserView(CyActivator.getBrowser());
			} catch (IPCException e) {
				e.printStackTrace();
			}
		if (browserView == null) {
			loadFailed = true;
			browserView = null;
		}
		return browserView;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public boolean loadSuccess() {
		return browserView != null;
	}
}
