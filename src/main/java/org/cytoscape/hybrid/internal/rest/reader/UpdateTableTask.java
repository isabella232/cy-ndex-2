package org.cytoscape.hybrid.internal.rest.reader;

import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

public class UpdateTableTask implements Task {

	private static final String UUID_COLUMN_NAME = "ndex.uuid";
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
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.LOCAL_ATTRS);
		
		if(table.getColumn(UUID_COLUMN_NAME) == null) {
			table.createColumn(UUID_COLUMN_NAME, String.class, true);
		}
		
		table.getRow(network.getSUID()).set(UUID_COLUMN_NAME, this.uuid);
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}

}
