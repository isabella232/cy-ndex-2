package org.cytoscape.cyndex2.internal.util;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

public class UpdateUtil {
	
	private static CyNetwork getRootNetwork(final Long suid, final CyNetworkManager networkManager, final CyRootNetworkManager rootNetworkManager) {
		Optional<CyRootNetwork> optional = networkManager.getNetworkSet().stream().map(net -> rootNetworkManager.getRootNetwork(net))
		.findFirst();
		return optional.isPresent() ? optional.get() : null;
	}
	
	public static UUID updateIsPossibleHelper(final Long suid, final boolean isCollection, final NdexRestClient nc, final NdexRestClientModelAccessLayer mal) throws Exception {

		final CyNetworkManager network_manager = CyServiceModule.getService(CyNetworkManager.class);
		final CyRootNetworkManager root_network_manager = CyServiceModule.getService(CyRootNetworkManager.class);
		final CyNetwork network = isCollection ? getRootNetwork(suid, network_manager, root_network_manager) : network_manager.getNetwork(suid);
		
		UUID ndexNetworkId = NDExNetworkManager.getUUID(network);
		
		if (ndexNetworkId == null) {
			throw new Exception(
					"NDEx network UUID not found. You can only update networks that were imported with CyNDEx2");
		}

		try {

			Map<String, Permissions> permissionTable = mal.getUserNetworkPermission(nc.getUserUid(), ndexNetworkId,
					false);
			if (permissionTable == null 
					|| permissionTable.size() == 0
					|| permissionTable.get(ndexNetworkId.toString()) == Permissions.READ)
				throw new Exception("You don't have permission to write to this network.");

		} catch (IOException | NdexException e) {
			throw new Exception("Unable to read network permissions. " + e.getMessage());
		}

		NetworkSummary ns = null;
		try {
			ns = mal.getNetworkSummaryById(ndexNetworkId);
			
			if (ns.getIsReadOnly())
				throw new Exception("The network is read only.");
			
			final Timestamp serverTimestamp = ns.getModificationTime();
			final Timestamp localTimestamp = NDExNetworkManager.getModificationTimeStamp(network);
			
			if (localTimestamp == null) {
				throw new Exception("Session file is missing timestamp; cannot safely update network on NDEx.");
			}
			
			final int timestampCompare = serverTimestamp.compareTo(localTimestamp);
			
			if (timestampCompare > 0) {
				throw new Exception("Network was modified on remote server.");
			}
		} catch (IOException | NdexException e) {
			throw new Exception(" An error occurred while checking permissions. " + e.getMessage());
		}
		return ndexNetworkId;
	}
}
