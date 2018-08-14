package org.cytoscape.cyndex2.internal.util;

import org.cytoscape.work.TaskMonitor;

public class StringResources {
	public static final String NDEX_SAVE = "Export Network to NDEx";
	public static final String NDEX_OPEN = "Import Network from NDEx";
	public static final String NDEX_SAVE_COLLECTION = "Export Collection to NDEx";
	
	public enum LoadBrowserStage {
		ENABLE_LOGGING(0.0f, "Enabling logging for JXBrowser"),
		DOWNLOAD_JAR(0.1f, "Downloading JxBrowser JAR from TeamDev"),
		EXTRACT_ZIP(0.3f, "Extracting zip file from JxBrowser JAR file"),
		EXTRACT_BINARY(0.4f, "Extracting JxBrowser binary from zip file"),
		CREATING_BROWSER(0.5f, "Creating browser instance"),
		STARTING_BROWSER(0.8f, "Browser created. Starting CyNDEx-2"),
		BROWSER_SETUP(0.9f, "Establishing local storage");
		
		private final float progress;
		private final String message;
		LoadBrowserStage(float progress, String message) {
			this.progress = progress;
			this.message = message;
		}
		public String getMessage() {
			return message;
		}
		public float getProgress() {
			return progress;
		}
		public void updateTaskMonitor(final TaskMonitor tm) {
			tm.setProgress(progress);
			tm.setStatusMessage(message);
		}
		
	}
	
}
