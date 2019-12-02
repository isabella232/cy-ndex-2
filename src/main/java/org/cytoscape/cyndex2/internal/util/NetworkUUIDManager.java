package org.cytoscape.cyndex2.internal.util;

import java.util.UUID;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;

public class NetworkUUIDManager {
	public static final String UUID_COLUMN = "NDEx UUID";
	
	public static void saveUUID(CyNetwork network, UUID uuid) {
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		
		if (table.getColumn(UUID_COLUMN) == null) {
			table.createColumn(UUID_COLUMN, String.class, false);
		}
		CyRow row = table.getRow(network.getSUID());
		System.out.println("Saving UUID for network" + network.getSUID());
		row.set(UUID_COLUMN, uuid.toString());
	}
	
	public static UUID getUUID(CyNetwork network) {
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		CyRow row = table.getRow(network.getSUID());
		if (row == null) {
			return null;
		}
		String uuid = row.get(UUID_COLUMN, String.class);
		if (uuid == null || uuid.isEmpty()) {
			return null;
		}
		return UUID.fromString(uuid);
	}
}
