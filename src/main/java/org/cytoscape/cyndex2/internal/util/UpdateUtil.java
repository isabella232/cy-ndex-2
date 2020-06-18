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

	private static CyNetwork getRootNetwork(final Long suid, final CyNetworkManager networkManager,
			final CyRootNetworkManager rootNetworkManager) {
		Optional<CyRootNetwork> optional = networkManager.getNetworkSet().stream()
				.map(net -> rootNetworkManager.getRootNetwork(net)).findFirst();
		return optional.isPresent() ? optional.get() : null;
	}

	public static CyNetwork getNetworkForSUID(final Long suid, final boolean isCollection) {
		final CyNetworkManager network_manager = CyServiceModule.getService(CyNetworkManager.class);
		final CyRootNetworkManager root_network_manager = CyServiceModule.getService(CyRootNetworkManager.class);
		return isCollection ? getRootNetwork(suid, network_manager, root_network_manager)
				: network_manager.getNetwork(suid);

	}

	public static UUID updateIsPossible(CyNetwork network, UUID uuid, final NdexRestClient nc,
			final NdexRestClientModelAccessLayer mal) throws Exception {
		return updateIsPossible(network, uuid, nc, mal, true);
	}

	public static UUID updateIsPossible(CyNetwork network, UUID uuid, final NdexRestClient nc,
			final NdexRestClientModelAccessLayer mal, final boolean checkTimestamp) throws Exception {

		if (uuid == null) {
			throw new Exception("UUID unknown. Can't find current Network in NDEx.");
		}

		try {

			Map<String, Permissions> permissionTable = mal.getUserNetworkPermission(nc.getUserUid(), uuid, false);
			if (permissionTable == null || permissionTable.size() == 0) {
				throw new Exception("Cannot find network.");
			} else if (permissionTable.get(uuid.toString()) == Permissions.READ) {
				throw new Exception("You don't have permission to write to this network.");
			}

		} catch (IOException | NdexException e) {
			throw new Exception("Unable to read network permissions. " + e.getMessage());
		}

		NetworkSummary ns = null;
		try {
			ns = mal.getNetworkSummaryById(uuid);

			if (ns.getIsReadOnly())
				throw new Exception("The network is read only.");

			if (checkTimestamp) {

				final Timestamp serverTimestamp = ns.getModificationTime();
				final Timestamp localTimestamp = NDExNetworkManager.getModificationTimeStamp(network);

				if (localTimestamp == null) {
					throw new Exception("Session file is missing timestamp.");
				}

				final int timestampCompare = serverTimestamp.compareTo(localTimestamp);

				if (timestampCompare > 0) {
					throw new Exception("Network was modified on remote server.");
				}
			}
		} catch (IOException | NdexException e) {
			throw new Exception("An error occurred while checking permissions. " + e.getMessage());
		}

		return uuid;
	}

	public static UUID updateIsPossibleHelper(final Long suid, final boolean isCollection, final NdexRestClient nc,
			final NdexRestClientModelAccessLayer mal) throws Exception {
		return updateIsPossibleHelper(suid, isCollection, nc, mal, true);
	}
	
	public static UUID updateIsPossibleHelper(final Long suid, final boolean isCollection, final NdexRestClient nc,
			final NdexRestClientModelAccessLayer mal, final boolean checkTimestamp) throws Exception {

		final CyNetwork network = getNetworkForSUID(suid, isCollection);

		UUID ndexNetworkId = NDExNetworkManager.getUUID(network);

		return updateIsPossible(network, ndexNetworkId, nc, mal, checkTimestamp);
	}
}
