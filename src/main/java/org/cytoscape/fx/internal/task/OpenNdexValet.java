package org.cytoscape.fx.internal.task;

import java.io.IOException;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class OpenNdexValet extends AbstractTask {

	private final OpenBrowser browser;

	private final String location;

	public OpenNdexValet(final CyApplicationConfiguration appConfig, final OpenBrowser browser) {
		this.browser = browser;

		final UIResourceGenerator resourceGenerator = new UIResourceGenerator(appConfig);
		try {
			resourceGenerator.extractPreviewTemplate();
		} catch (IOException e) {
			throw new IllegalStateException("Could not extract UI template", e);
		}

		this.location = resourceGenerator.getResourceLocation();
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {

		final String targetIndexFile = "file://" + this.location + "/index.html";
		System.out.println("Targeti2: " + targetIndexFile);
//		this.browser.openURL(targetIndexFile);
	}

}
