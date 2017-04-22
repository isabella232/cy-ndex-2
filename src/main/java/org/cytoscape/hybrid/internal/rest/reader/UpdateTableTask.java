package org.cytoscape.hybrid.internal.rest.reader;

import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

public class UpdateTableTask implements Task {

	private final CyNetworkReader reader;
	
	
	public UpdateTableTask(final CyNetworkReader reader) {
		// TODO Auto-generated constructor stub
		this.reader = reader;
	}
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		final CyNetwork[] networks = reader.getNetworks();
		System.out.println("NETWORk = " + networks[0].getSUID());
		
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}

}
