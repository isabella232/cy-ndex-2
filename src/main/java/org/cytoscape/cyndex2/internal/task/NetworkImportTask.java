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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.cxio.aspects.datamodels.CartesianLayoutElement;
import org.cxio.aspects.datamodels.CyGroupsElement;
import org.cxio.aspects.datamodels.CyTableColumnElement;
import org.cxio.aspects.datamodels.CyViewsElement;
import org.cxio.aspects.datamodels.CyVisualPropertiesElement;
import org.cxio.aspects.datamodels.HiddenAttributesElement;
import org.cxio.aspects.datamodels.SubNetworkElement;
import org.cxio.core.interfaces.AspectElement;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.singletons.CXInfoHolder;
import org.cytoscape.cyndex2.internal.singletons.CyObjectManager;
import org.cytoscape.cyndex2.internal.singletons.NetworkManager;
import org.cytoscape.cyndex2.internal.strings.ErrorMessage;
import org.cytoscape.cyndex2.io.cxio.CxImporter;
import org.cytoscape.cyndex2.io.cxio.reader.CxToCy;
import org.cytoscape.cyndex2.io.cxio.reader.ViewMaker;
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
import org.cytoscape.work.TaskMonitor;
import org.ndexbio.model.cx.NiceCXNetwork;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

public class NetworkImportTask extends AbstractTask {

	final NdexRestClientModelAccessLayer mal;
	final NetworkSummary networkSummary;
	private Long suid = null;
	private String accessKey = null;

	public NetworkImportTask(String userId, String password, String serverUrl, UUID uuid, String accessKey)
			throws IOException, NdexException {
		super();
		NdexRestClient client = new NdexRestClient(userId, password, serverUrl, CyActivator.getAppName()+"/"+CyActivator.getAppVersion());
		mal = new NdexRestClientModelAccessLayer(client);
		networkSummary = mal.getNetworkSummaryById(uuid, accessKey);
		this.accessKey = accessKey;
	}
	
	public NetworkImportTask(String serverUrl, UUID uuid, String accessKey) throws IOException, NdexException{
		super();
		NdexRestClient client = new NdexRestClient(null,null,serverUrl, CyActivator.getAppName()+"/"+CyActivator.getAppVersion());
		mal = new NdexRestClientModelAccessLayer(client);
		networkSummary = mal.getNetworkSummaryById(uuid, accessKey);
		this.accessKey = accessKey;
	}
	
/*	public void setAccessKey(String accessKey){
		this.accessKey = accessKey;
	} */
	
	private void createCyNetworkFromCX(InputStream cxStream) throws IOException {

		// Create the CyNetwork to copy to.
		CyNetworkFactory networkFactory = CyObjectManager.INSTANCE.getNetworkFactory();
		CxToCy cxToCy = new CxToCy();
		CxImporter cxImporter = new CxImporter();

		NiceCXNetwork niceCX = cxImporter.getCXNetworkFromStream(cxStream);

		boolean doLayout;
		boolean hasCoords = niceCX.getNodeAssociatedAspect(CartesianLayoutElement.ASPECT_NAME) != null;

		if (hasCoords) {
			doLayout = false;
		} else //if (networkSummary.getEdgeCount() < 5000) {
			doLayout = true;
	/*	} else {
			JFrame parent = CyObjectManager.INSTANCE.getApplicationFrame();
			int response = JOptionPane.showConfirmDialog(parent,
					"Do you want to create a view for your large network? This could take a while.",
					"Importing Large Network", JOptionPane.YES_NO_OPTION);
			doLayout = response == JOptionPane.YES_OPTION;
		} */

		List<CyNetwork> networks = cxToCy.createNetwork(niceCX, null, networkFactory, null, true);

		if (!niceCX.getOpaqueAspectTable().containsKey(SubNetworkElement.ASPECT_NAME)) {
			// populate the CXInfoHolder object.
			CXInfoHolder cxInfoHolder = new CXInfoHolder();

			cxInfoHolder.setNamespaces(niceCX.getNamespaces());

			for (Map.Entry<Long, CyNode> entry : cxToCy.get_cxid_to_cynode_map().entrySet()) {
				cxInfoHolder.addNodeMapping(entry.getValue().getSUID(), entry.getKey());
			}

			for (Map.Entry<Long, CyEdge> entry : cxToCy.get_cxid_to_cyedge_map().entrySet()) {
				cxInfoHolder.addEdgeMapping(entry.getValue().getSUID(), entry.getKey());
			}

			cxInfoHolder.setOpaqueAspectsTable(niceCX.getOpaqueAspectTable().entrySet().stream()
					.filter(map -> (!map.getKey().equals(SubNetworkElement.ASPECT_NAME)
							&& !map.getKey().equals(CyGroupsElement.ASPECT_NAME))
							&& !map.getKey().equals(CyViewsElement.ASPECT_NAME)
							&& !map.getKey().equals(CyVisualPropertiesElement.ASPECT_NAME)
							&& !map.getKey().equals(CartesianLayoutElement.ASPECT_NAME)
							&& !map.getKey().equals(CyTableColumnElement.ASPECT_NAME)
							&& !map.getKey().equals(HiddenAttributesElement.ASPECT_NAME))
					.collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue())));

			cxInfoHolder.setProvenance(niceCX.getProvenance());
			cxInfoHolder.setMetadata(niceCX.getMetadata());
			cxInfoHolder.setNetworkId(networkSummary.getExternalId());
			Collection<AspectElement> subNets = niceCX.getOpaqueAspectTable().get(SubNetworkElement.ASPECT_NAME);

			cxInfoHolder.setSubNetCount(subNets == null ? 0 : subNets.size());

			for (CyNetwork subNetwork : networks) {
				NetworkManager.INSTANCE.setCXInfoHolder(subNetwork.getSUID(), cxInfoHolder);
			}
		}

		CyRootNetwork rootNetwork = ((CySubNetwork) networks.get(0)).getRootNetwork();
		suid = rootNetwork.getSUID();

		NetworkManager.INSTANCE.addNetworkUUID(networks.size() == 1 ? networks.get(0).getSUID() : rootNetwork.getSUID(),
				networkSummary.getExternalId());

		String collectionName = networkSummary.getName();
		rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, collectionName);

		for (CyNetwork cyNetwork : networks) {
			CyObjectManager.INSTANCE.getNetworkManager().addNetwork(cyNetwork);

			if (doLayout || hasCoords) {
				CyNetworkViewFactory nvf = CyObjectManager.INSTANCE.getNetworkViewFactory();
				RenderingEngineManager rem = CyObjectManager.INSTANCE.getRenderingEngineManager();
				VisualMappingManager vmm = CyObjectManager.INSTANCE.getVisualMappingManager();
				VisualStyleFactory vsf = CyObjectManager.INSTANCE.getVisualStyleFactory();

				//CyNetworkView cyNetworkView =
						ViewMaker.makeView(cyNetwork, cxToCy, collectionName, nvf, rem, vmm, vsf,
						doLayout);

			//	CyObjectManager.INSTANCE.getNetworkViewManager().addNetworkView(cyNetworkView);
			//	cyNetworkView.fitContent();
			}
		}
	}

	public Long getSUID() {
		return suid;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws NetworkImportException {

		SwingWorker<Integer, Integer> worker = new SwingWorker<Integer, Integer>() {

			@Override
			protected Integer doInBackground() throws NetworkImportException {

				{

					// For entire network, we will query again, hence will check
					// credential
					boolean success = true; // selectedServer.check(mal);
					if (success) {
						UUID id = networkSummary.getExternalId();
						try {
							InputStream cxStream = null;
							if (accessKey == null)
								 cxStream = mal.getNetworkAsCXStream(id);
							else
								cxStream = mal.getNetworkAsCXStream(id, accessKey);
							createCyNetworkFromCX(cxStream); 
						} catch (IOException ex) {
							throw new NetworkImportException(ErrorMessage.failedToParseJson);
						} catch (RuntimeException ex2) {
							throw new NetworkImportException(ex2.getMessage());
						} catch (NdexException e) {
							throw new NetworkImportException("Unable to read network from NDEx");
						}
					} else {
						throw new NetworkImportException(ErrorMessage.failedServerCommunication);
					}
				}
				return 1;
			}

		};
		worker.execute();
		// This is a hack, to wait until the SUID of the root is available to
		// return

		while (suid == null) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				throw new NetworkImportException("Failed to wait. This should never happen.");
			}
		}
		return;
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

}
