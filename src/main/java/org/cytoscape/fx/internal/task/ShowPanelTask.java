package org.cytoscape.fx.internal.task;

import java.awt.Container;
import java.awt.Dimension;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.fx.internal.ui.NdexMainPanel;
import org.cytoscape.fx.internal.ws.ProcessManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShowPanelTask extends AbstractTask {

	private static final Logger logger = LoggerFactory.getLogger(ShowPanelTask.class);

	private final CySwingApplication cySwingApplicationServiceRef;
	private final CyServiceRegistrar registrar;
	private final ProcessManager pm;

	private final String location;

	public ShowPanelTask(final CyServiceRegistrar registrar, final CySwingApplication cySwingApplicationServiceRef,
			final CyApplicationConfiguration appConfig, final ProcessManager pm) {

		this.cySwingApplicationServiceRef = cySwingApplicationServiceRef;
		this.registrar = registrar;
		this.pm = pm;

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
		taskMonitor.setTitle("NDEx Valet");
		taskMonitor.setStatusMessage("NDEx Valet Window Opended.  Blocked until it terminated.");
		taskMonitor.setProgress(0.0);
		
		String targetIndexFile = "file://" + this.location + "/index.html";
		// targetIndexFile = "http://html5test.com/index.html";

//		cySwingApplicationServiceRef.getJMenuBar().setEnabled(false);
//		cySwingApplicationServiceRef.getJToolBar().setEnabled(false);
//		cySwingApplicationServiceRef.getJFrame().setEnabled(false);
//		cySwingApplicationServiceRef.getCytoPanel(CytoPanelName.WEST).getThisComponent().setEnabled(false);
//		
//		System.out.println(targetIndexFile);
//		System.out.println("---------- Opening loading ------------");
//		String cmd = "/Applications/MacVim.app/Contents/MacOS/MacVim";
//		System.out.println("---------- Map opened ------------");
//		Process p = Runtime.getRuntime().exec(cmd);
//		System.out.println("Map alive? " + p.isAlive());
//		while(p.isAlive()) {
//			Thread.sleep(5000);
//			System.out.println("---------- Map Not finished yet: " + p.isAlive());
//			
//		}
//		cySwingApplicationServiceRef.getJMenuBar().setEnabled(true);
//		cySwingApplicationServiceRef.getJToolBar().setEnabled(true);
//		cySwingApplicationServiceRef.getJFrame().setEnabled(true);
//		cySwingApplicationServiceRef.getCytoPanel(CytoPanelName.WEST).getThisComponent().setEnabled(true);
//		
//		System.out.println("---------- Map finished: " + p.isAlive());

		cySwingApplicationServiceRef.getJFrame().requestFocus();
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				final NdexMainPanel panel = new NdexMainPanel(cySwingApplicationServiceRef, pm);
				panel.setPreferredSize(new Dimension(600, 1000));
				registrar.registerAllServices(panel, new Properties());
				System.out.println("---------- Finished loading ------------");
				CytoPanel parent = cySwingApplicationServiceRef.getCytoPanel(CytoPanelName.SOUTH_WEST);
				
				parent.setState(CytoPanelState.DOCK);
				parent.setSelectedIndex(parent.indexOfComponent(panel));
				Container splitpane = parent.getThisComponent().getParent();
				System.out.println(splitpane);
				((JSplitPane)splitpane).setDividerLocation(0.8);
			}
		});
	}
	
	@Override
	public void cancel() {
		
	}
}
