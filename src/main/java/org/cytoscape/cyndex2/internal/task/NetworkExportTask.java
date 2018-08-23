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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.io.cxio.writer.CxNetworkWriter;
import org.cytoscape.cyndex2.internal.rest.endpoints.impl.NdexNetworkResourceImpl.Monitor;
import org.cytoscape.cyndex2.internal.rest.parameter.NdexBasicSaveParameter;
import org.cytoscape.cyndex2.internal.rest.response.NdexBaseResponse;
import org.cytoscape.cyndex2.internal.singletons.CXInfoHolder;
import org.cytoscape.cyndex2.internal.singletons.CyObjectManager;
import org.cytoscape.cyndex2.internal.util.HeadlessTaskMonitor;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.swing.DialogTaskManager;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.core.JsonProcessingException;

public class NetworkExportTask extends AbstractTask implements ObservableTask {

	private final CyNetwork network;
	private final NdexBasicSaveParameter params;
	private boolean isUpdate;
	private final NdexRestClientModelAccessLayer mal;
	private UUID networkUUID = null;
	private final long suid;
	private boolean writeCollection = false;

	public NetworkExportTask(CyNetwork network, NdexBasicSaveParameter params, boolean isUpdate)
			throws JsonProcessingException, IOException, NdexException {
		super();
		this.suid = network.getSUID();
		this.params = params;
		this.isUpdate = isUpdate;
		
		// network must refer to a subnetwork, but writeCollection is used to
		// export the entire collection
		if (network instanceof CyRootNetwork) {
			writeCollection = true;
			this.network = ((CyRootNetwork) network).getSubNetworkList().get(0);
		} else {
			this.network = network;
		}

		NdexRestClient client = new NdexRestClient(params.username, params.password, params.serverUrl,
				CyActivator.getAppName() + "/" + CyActivator.getAppVersion());
		mal = new NdexRestClientModelAccessLayer(client);

	}

	private void prepareToWriteNetworkToCXStream(CyNetwork cyNetwork, PipedOutputStream out, boolean isUpdateNdex) throws FileNotFoundException, IOException {
		VisualMappingManager vmm = CyObjectManager.INSTANCE.getVisualMappingManager();
		final CyNetworkViewManager nvm = CyObjectManager.INSTANCE.getNetworkViewManager();
		// final CyGroupManager gm = CyObjectManager.INSTANCE.getCyGroupManager();
		final VisualLexicon lexicon = CyObjectManager.INSTANCE.getDefaultVisualLexicon();

		CxNetworkWriter writer = new CxNetworkWriter(out, cyNetwork, vmm, nvm, /* gm, */ lexicon, isUpdateNdex);

		writer.setWriteSiblings(writeCollection);

		TaskIterator ti = new TaskIterator(writer);
		DialogTaskManager tm = CyObjectManager.INSTANCE.getTaskManager();
		tm.execute(ti);
//		writer.run(new HeadlessTaskMonitor());
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws NetworkExportException {
		synchronized (Monitor.INSTANCE) {
			networkUUID = null;
			taskMonitor.setTitle("Exporting " + (writeCollection ? "collection" : "network") + " to NDEx...");
			CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();

			String collectionName = rootNetwork.getRow(rootNetwork).get(CyNetwork.NAME, String.class);
			String networkName = network.getRow(network).get(CyNetwork.NAME, String.class);

			String uploadName = (params.metadata != null && params.metadata.containsKey(CyNetwork.NAME))
					? params.metadata.get(CyNetwork.NAME)
					: (writeCollection ? collectionName : networkName);

			CyNetwork my_net = writeCollection ? rootNetwork : network;
			my_net.getRow(my_net).set(CyNetwork.NAME, uploadName);


			try (PipedInputStream in = new PipedInputStream(); PipedOutputStream out = new PipedOutputStream(in)) {

				taskMonitor.setStatusMessage("Preparing to convert network to CX");
				prepareToWriteNetworkToCXStream(network, out, false);

				taskMonitor.setProgress(.3);
				taskMonitor.setStatusMessage("Converting network to CX");
				
				if (!isUpdate) {
					networkUUID = mal.createCXNetwork(in);
				} else {
					prepareForUpdate(my_net);
					mal.updateCXNetwork(networkUUID, in);
				}
				
				taskMonitor.setProgress(.9);
				taskMonitor.setStatusMessage("Saving changes to network in Cytoscape");
				// Revert names back to original?
				rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, collectionName);
				network.getRow(network).set(CyNetwork.NAME, networkName);
				
				if (networkUUID == null) {
					throw new NetworkExportException("There was a problem exporting the network!");
				}
				
				// add UUID to hidden network table
				CXInfoHolder.addNetworkUUID(my_net, mal.getNdexRestClient().getBaseroute(), networkUUID);

				CyObjectManager.INSTANCE.getApplicationFrame().revalidate();

				taskMonitor.setProgress(1.0f);

			} catch (NetworkUpdateException e) {
				throw new NetworkExportException(
						"Only networks imported from CyNDEx2 can be updated. Error: " + e.getMessage());
			} catch (IOException e) {
				throw new NetworkExportException("Failed to create CX stream for network. Error: " + e.getMessage());
			} catch (Exception e) {
				throw new NetworkExportException(
						"An error occurred loading the network to NDEx:" + e.getMessage());
			} finally {
				Monitor.INSTANCE.notifyAll();
			}

		}

	}

	private void prepareForUpdate(CyNetwork network) throws NetworkUpdateException {

		networkUUID = CXInfoHolder.getNdexNetworkId(network);
		if (networkUUID == null) {
			throw new NetworkUpdateException(
					"NDEx network UUID not found. You can only update networks that were imported with CyNDEx2");
		}
	}

	public class NetworkExportException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = -4168495871463038598L;

		public NetworkExportException(String message) {
			super(message);
		}
	}

	public class NetworkUpdateException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public NetworkUpdateException(String message) {
			super(message);
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		if (networkUUID == null)
			return null;

		if (type.equals(NdexBaseResponse.class)) {
			return (R) new NdexBaseResponse(suid, networkUUID.toString());
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
