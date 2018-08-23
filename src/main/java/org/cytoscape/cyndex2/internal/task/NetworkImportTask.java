/*
 * Copyright (c) 2014, the Cytoscape Consortium and the Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.cytoscape.cyndex2.internal.task;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cxio.aspects.datamodels.CartesianLayoutElement;
import org.cxio.aspects.datamodels.CyGroupsElement;
import org.cxio.aspects.datamodels.CyTableColumnElement;
import org.cxio.aspects.datamodels.CyViewsElement;
import org.cxio.aspects.datamodels.CyVisualPropertiesElement;
import org.cxio.aspects.datamodels.HiddenAttributesElement;
import org.cxio.aspects.datamodels.SubNetworkElement;
import org.cxio.core.interfaces.AspectElement;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.io.cxio.CxImporter;
import org.cytoscape.cyndex2.internal.io.cxio.reader.CxToCy;
import org.cytoscape.cyndex2.internal.io.cxio.reader.ViewMaker;
import org.cytoscape.cyndex2.internal.singletons.CXInfoHolder;
import org.cytoscape.cyndex2.internal.singletons.CyObjectManager;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexNetworkResourceImpl.Monitor;
import org.cytoscape.cyndex2.internal.rest.response.NdexBaseResponse;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.core.JsonProcessingException;

public class NetworkImportTask extends AbstractTask implements ObservableTask {

	final NdexRestClientModelAccessLayer mal;
	final NetworkSummary networkSummary;
	final private String serverUrl;
	private String accessKey = null;
	protected InputStream cxStream;

	// Result objects
	private Long suid = null;

	private NetworkImportTask(NdexRestClientModelAccessLayer mal, UUID uuid, String accessKey)
			throws IOException, NdexException {
		super();
		this.mal = mal;
		cxStream = null;
		this.serverUrl = mal.getNdexRestClient().getBaseroute();
		this.networkSummary = mal.getNetworkSummaryById(uuid, accessKey);
	}

	public static NetworkImportTask withLogin(String userId, String password, String serverUrl, UUID uuid,
			String accessKey) throws JsonProcessingException, IOException, NdexException {
		NdexRestClient client = new NdexRestClient(userId, password, serverUrl,
				CyActivator.getAppName() + "/" + CyActivator.getAppVersion());
		return new NetworkImportTask(new NdexRestClientModelAccessLayer(client), uuid, accessKey);
	}

	public static NetworkImportTask withIdToken(String serverUrl, UUID uuid, String accessKey, String idToken)
			throws IOException, NdexException {
		NdexRestClient client = new NdexRestClient(null, null, serverUrl,
				CyActivator.getAppName() + "/" + CyActivator.getAppVersion());
		if (idToken != null)
			client.signIn(idToken);
		return new NetworkImportTask(new NdexRestClientModelAccessLayer(client), uuid, accessKey);
	}

	public NetworkImportTask(InputStream in) {
		super();
		networkSummary = null;
		mal = null;
		this.serverUrl = "";
		cxStream = in;
	}

	protected void createCyNetworkFromCX(NiceCXNetwork niceCX, TaskMonitor taskMonitor) throws IOException {

		taskMonitor.setStatusMessage("Parsing CX network from NDEx");
		// Create the CyNetwork to copy to.
		CyNetworkFactory networkFactory = CyObjectManager.INSTANCE.getNetworkFactory();
		CxToCy cxToCy = new CxToCy();

		// if has coordinates, create view, do not layout
		boolean hasCoords = niceCX.getNodeAssociatedAspect(CartesianLayoutElement.ASPECT_NAME) != null;
		// else if < 50k edges, create view and layout
		// ViewMaker uses grid if edges < 10k else force-directed
		boolean doLayout = !hasCoords && (networkSummary == null || networkSummary.getEdgeCount() < 50000);

		taskMonitor.setProgress(.7);
		taskMonitor.setStatusMessage("Building Cytoscape networks");
		List<CyNetwork> networks = cxToCy.createNetwork(niceCX, null, networkFactory, null);
		boolean isCollection = niceCX.getOpaqueAspectTable().containsKey(SubNetworkElement.ASPECT_NAME);

		CyRootNetwork rootNetwork = ((CySubNetwork) networks.get(0)).getRootNetwork();

		CyNetwork my_net = networks.get(0);

		taskMonitor.setProgress(.8);
		taskMonitor.setStatusMessage("Storing hidden NDEx attributes");
		Map<String, Collection<AspectElement>> opaqueTable = niceCX.getOpaqueAspectTable().entrySet().stream()
				.filter(map -> (!map.getKey().equals(SubNetworkElement.ASPECT_NAME)
						&& !map.getKey().equals(CyGroupsElement.ASPECT_NAME))
						&& !map.getKey().equals(CyViewsElement.ASPECT_NAME)
						&& !map.getKey().equals(CyVisualPropertiesElement.ASPECT_NAME)
						&& !map.getKey().equals(CartesianLayoutElement.ASPECT_NAME)
						&& !map.getKey().equals(CyTableColumnElement.ASPECT_NAME)
						&& !map.getKey().equals(HiddenAttributesElement.ASPECT_NAME))
				.collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));

		// per network attributes
		for (CyNetwork net : networks) {
			for (Map.Entry<Long, CyNode> entry : cxToCy.get_cxid_to_cynode_map().entrySet()) {
				if (net.containsNode(entry.getValue())) {
					CXInfoHolder.addNodeMapping(net, entry.getValue().getSUID(), entry.getKey());
				}
			}
			for (Map.Entry<Long, CyEdge> entry : cxToCy.get_cxid_to_cyedge_map().entrySet()) {
				if (net.containsEdge(entry.getValue())) {
					CXInfoHolder.addEdgeMapping(net, entry.getValue().getSUID(), entry.getKey());
				}
			}
		}

		if (!isCollection) {
			suid = networks.get(0).getSUID();
			CXInfoHolder.setNamespaces(my_net, niceCX.getNamespaces());
			CXInfoHolder.setOpaqueAspectsTable(my_net, opaqueTable);
			CXInfoHolder.setProvenance(my_net, niceCX.getProvenance());
			CXInfoHolder.setMetadata(my_net, niceCX.getMetadata());
		}else {
			suid = rootNetwork.getSUID();
		}

		if (networkSummary != null) {
			CyNetwork net = isCollection ? ((CySubNetwork) my_net).getRootNetwork() : my_net;
			CXInfoHolder.addNetworkUUID(net, serverUrl, networkSummary.getExternalId());
		}

		

		String collectionName = (networkSummary != null) ? networkSummary.getName() : niceCX.getNetworkName();
		rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, collectionName);

		taskMonitor.setProgress(.9);
		taskMonitor.setStatusMessage("Creating views for networks");
		for (CyNetwork cyNetwork : networks) {
			CyObjectManager.INSTANCE.getNetworkManager().addNetwork(cyNetwork);

			if (doLayout || hasCoords) {
				CyNetworkViewFactory nvf = CyObjectManager.INSTANCE.getNetworkViewFactory();
				RenderingEngineManager rem = CyObjectManager.INSTANCE.getRenderingEngineManager();
				VisualMappingManager vmm = CyObjectManager.INSTANCE.getVisualMappingManager();
				VisualStyleFactory vsf = CyObjectManager.INSTANCE.getVisualStyleFactory();

				ViewMaker.makeView(cyNetwork, cxToCy, collectionName, nvf, rem, vmm, vsf, doLayout);
			}
		}
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws NetworkImportException {
		synchronized (Monitor.INSTANCE) {
			try {
				taskMonitor.setStatusMessage("Fetching network from NDEx");
				if (cxStream == null) {
					UUID uuid = networkSummary.getExternalId();
					if (accessKey == null)
						cxStream = mal.getNetworkAsCXStream(uuid);
					else
						cxStream = mal.getNetworkAsCXStream(uuid, accessKey);
				}

				if (cxStream == null) {
					throw new NdexException("Failed to get CX for network");
				}
				taskMonitor.setProgress(.4);

				checkCancelled();

				CxImporter cxImporter = new CxImporter();
				NiceCXNetwork niceCX = cxImporter.getCXNetworkFromStream(cxStream);

				checkCancelled();

				taskMonitor.setTitle("Importing Network from NDEx...");
				taskMonitor.setProgress(.5);
				createCyNetworkFromCX(niceCX, taskMonitor);
			} catch(ImportCancelledException e) {
				
			} catch (IOException ex) {
				throw new NetworkImportException("Failed to parse JSON from NDEx source.");
			} catch (RuntimeException ex2) {
				throw new NetworkImportException(ex2.getMessage());
			} catch (NdexException e) {
				throw new NetworkImportException("NDEx Network Import Error: " + e.getMessage());
			} finally {
				Monitor.INSTANCE.notifyAll();
			}
		}
	}

	private void checkCancelled() throws ImportCancelledException {
		if (cancelled) {
			try {
				cxStream.close();
			} catch (IOException e) {
				// ignore
			}
			throw new ImportCancelledException();
		}
	}

	public class NetworkImportException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = -1186105413302386171L;

		public NetworkImportException(String message) {
			super(message);
		}
	}
	
	public class ImportCancelledException extends Exception {

		private static final long serialVersionUID = -4504656577026124504L;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		if (suid == null) {
			return null;
		}
		if (type.equals(NdexBaseResponse.class)) {
			UUID uuid = networkSummary.getExternalId();
			return (R) new NdexBaseResponse(suid, uuid.toString());
		}
		return null;
	}

	@Override
	public List<Class<?>> getResultClasses() {
		ArrayList<Class<?>> list = new ArrayList<>();
		list.add(NdexBaseResponse.class);
		return list;
	}

}
