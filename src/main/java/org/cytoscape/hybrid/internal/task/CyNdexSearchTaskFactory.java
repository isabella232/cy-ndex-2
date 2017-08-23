package org.cytoscape.hybrid.internal.task;

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;


public class CyNdexSearchTaskFactory extends AbstractNetworkSearchTaskFactory {

	private static final String ID = "netsearchtest.test-c";
	private static final String NAME = "CyNDEX-2";
	private static final String DESCRIPTION = "Import networks from CyNDEX";
	private URL website;
	
	
	private final CyServiceRegistrar serviceRegistrar;
	
	public CyNdexSearchTaskFactory(CyServiceRegistrar serviceRegistrar, ImageIcon icon) {
		super(ID, NAME, icon);
		this.serviceRegistrar = serviceRegistrar;
		
		try {
			website = new URL("https://github.com/chrtannus/netsearch-test/blob/master/src/main/java/org/cytoscape/netsearchtest/internal/task/CustomQueryTaskFactory.java");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public JComponent getOptionsComponent() {
		return null;
	}
	
	@Override
	public TaskIterator createTaskIterator() {
		return new TaskIterator(new AbstractTask() {
			@Override
			public void run(TaskMonitor tm) throws Exception {
				
			}
		});
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public URL getWebsite() {
		return website;
	}

	@Override
	public TaskObserver getTaskObserver() {
		return null;
	}

	@Override
	public boolean isReady() {
		return true;
	}
	
	
}
