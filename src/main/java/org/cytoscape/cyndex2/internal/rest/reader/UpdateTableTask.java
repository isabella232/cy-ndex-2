package org.cytoscape.cyndex2.internal.rest.reader;

import org.cytoscape.cyndex2.internal.rest.endpoints.NdexStatusResource;
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
		
		CyNetwork network = networks[0];
		if(network instanceof CySubNetwork) {
			CySubNetwork subnet = (CySubNetwork) network;
			
			final CyRootNetwork root = subnet.getRootNetwork();
			CyTable table = root.getDefaultNetworkTable();

			if (table.getColumn(NdexStatusResource.NDEX_UUID_TAG) == null) {
				table.createColumn(NdexStatusResource.NDEX_UUID_TAG, String.class, true);
			}

			table.getRow(root.getSUID()).set(NdexStatusResource.NDEX_UUID_TAG, this.uuid);
		}
	}

	@Override
	public void cancel() {
		
	}
}
