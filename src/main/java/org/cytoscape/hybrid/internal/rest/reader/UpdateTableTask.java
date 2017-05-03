package org.cytoscape.hybrid.internal.rest.reader;

import org.cytoscape.hybrid.internal.rest.NdexClient;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

public class UpdateTableTask implements Task {

	private final CyNetworkReader reader;
	
	private String uuid;
	
	
	public UpdateTableTask(final CyNetworkReader reader) {
		// TODO Auto-generated constructor stub
		this.reader = reader;
	}
	
	/**
	 * UUID should be set before calling this task.
	 * 
	 * @param uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		
		final CyNetwork[] networks = reader.getNetworks();
		System.out.println("NETWORk = " + networks[0].getSUID());
		
		CyNetwork network = networks[0];
		if(network instanceof CySubNetwork) {
			CySubNetwork subnet = (CySubNetwork) network;
			
			final CyRootNetwork root = subnet.getRootNetwork();
			CyTable table = root.getDefaultNetworkTable();

			if (table.getColumn(NdexClient.UUID_COLUMN_NAME) == null) {
				table.createColumn(NdexClient.UUID_COLUMN_NAME, String.class, true);
			}

			table.getRow(root.getSUID()).set(NdexClient.UUID_COLUMN_NAME, this.uuid);
		}
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}

}
