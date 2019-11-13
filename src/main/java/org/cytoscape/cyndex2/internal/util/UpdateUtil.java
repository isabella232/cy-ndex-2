package org.cytoscape.cyndex2.internal.util;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExBasicSaveParameters;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

public class UpdateUtil {
	public static UUID updateIsPossibleHelper(final Long suid, String username, String password, String url) throws Exception {

		CyNetworkManager network_manager = CyServiceModule.getService(CyNetworkManager.class);
		CyNetwork network = network_manager.getNetwork(suid);
		UUID ndexNetworkId = NetworkUUIDManager.getUUID(network);
		
		if (ndexNetworkId == null) {
			throw new Exception(
					"NDEx network UUID not found. You can only update networks that were imported with CyNDEx2");
		}

		final NdexRestClient nc = new NdexRestClient(username, password, url,
				CyActivator.getAppName() + "/" + CyActivator.getAppVersion());
		final NdexRestClientModelAccessLayer mal = new NdexRestClientModelAccessLayer(nc);
		try {

			Map<String, Permissions> permissionTable = mal.getUserNetworkPermission(nc.getUserUid(), ndexNetworkId,
					false);
			if (permissionTable == null || permissionTable.get(ndexNetworkId.toString()) == Permissions.READ)
				throw new Exception("You don't have permission to write to this network.");

		} catch (IOException | NdexException e) {
			throw new Exception("Unable to read network permissions. " + e.getMessage());
		}

		NetworkSummary ns = null;
		try {
			ns = mal.getNetworkSummaryById(ndexNetworkId);
			if (ns.getIsReadOnly())
				throw new Exception("The network is read only.");

		} catch (IOException | NdexException e) {
			throw new Exception(" An error occurred while checking permissions. " + e.getMessage());
		}
		return ndexNetworkId;
	}
}
