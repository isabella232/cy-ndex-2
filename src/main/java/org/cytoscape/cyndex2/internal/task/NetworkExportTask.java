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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.UUID;

import org.cytoscape.group.CyGroupManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.cyndex2.internal.rest.parameter.NdexSaveParameters;
import org.cytoscape.cyndex2.internal.singletons.CXInfoHolder;
import org.cytoscape.cyndex2.internal.singletons.CyObjectManager;
import org.cytoscape.cyndex2.internal.singletons.NetworkManager;
import org.cytoscape.cyndex2.io.cxio.writer.CxNetworkWriter;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.swing.DialogTaskManager;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

public class NetworkExportTask extends AbstractTask {

	private final CyNetwork network;
	private final NdexSaveParameters params;
	private boolean isUpdate;
	private final NdexRestClientModelAccessLayer mal;
	private UUID networkUUID = null;
	private boolean writeCollection = false;

	public NetworkExportTask(CyNetwork network, NdexSaveParameters params, boolean isUpdate) {
		super();
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

		NdexRestClient client = new NdexRestClient(params.username, params.password, params.serverUrl);
		mal = new NdexRestClientModelAccessLayer(client);
	}

	private void prepareToWriteNetworkToCXStream(CyNetwork cyNetwork, PipedOutputStream out, boolean isUpdate) {
		VisualMappingManager vmm = CyObjectManager.INSTANCE.getVisualMappingManager();
		final CyNetworkViewManager nvm = CyObjectManager.INSTANCE.getNetworkViewManager();
		final CyGroupManager gm = CyObjectManager.INSTANCE.getCyGroupManager();
		final VisualLexicon lexicon = CyObjectManager.INSTANCE.getDefaultVisualLexicon();

		CxNetworkWriter writer = new CxNetworkWriter(out, cyNetwork, vmm, nvm, gm, lexicon, isUpdate);
		
		writer.setWriteSiblings(writeCollection);

		TaskIterator ti = new TaskIterator(writer);
		DialogTaskManager tm = CyObjectManager.INSTANCE.getTaskManager();
		tm.execute(ti);
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws NetworkExportException {
		networkUUID = null;
		/*
		if (network.getEdgeCount() > 100000) {
			JFrame parent = CyObjectManager.INSTANCE.getApplicationFrame();
			String msg = "You have chosen to upload a network that has more than 10,000 edges.\n";
			msg += "The upload will occur in the background and you can continue working,\n";
			msg += "but it may take a while to appear in NDEx. Would you like to proceed?";
			String dialogTitle = "Proceed?";
			int choice = JOptionPane.showConfirmDialog(parent, msg, dialogTitle, JOptionPane.YES_NO_OPTION);
			if (choice == JOptionPane.NO_OPTION)
				return;
		}
		*/
		CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();

		
		String collectionName = rootNetwork.getRow(rootNetwork).get(CyNetwork.NAME, String.class);
		String networkName = network.getRow(network).get(CyNetwork.NAME, String.class);

		String uploadName = (params.metadata != null && params.metadata.containsKey(CyNetwork.NAME))
				? params.metadata.get(CyNetwork.NAME) : (writeCollection ? collectionName : networkName);

		Long suid;
		// Set root or network name
		if (writeCollection) {
			rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, uploadName);
			suid = rootNetwork.getSUID();
		}else{
			network.getRow(network).set(CyNetwork.NAME, uploadName);
			suid = network.getSUID();
		}
		PipedInputStream in = null;
		PipedOutputStream out = null;

		try {
			in = new PipedInputStream();
			out = new PipedOutputStream(in);

			prepareToWriteNetworkToCXStream(network, out, false);
			if (!isUpdate) {
				networkUUID = mal.createCXNetwork(in);
				NetworkManager.INSTANCE.addNetworkUUID(suid, networkUUID);
			} else {
				prepareForUpdate(suid);
				mal.updateCXNetwork(networkUUID, in);
			}
		} catch (NetworkUpdateException e) {
			throw new NetworkExportException("Only networks imported from CyNDEx2 can be updated.");
		} catch (IOException e) {
			throw new NetworkExportException("Failed to create CX stream for network.");
		} catch (Exception e) {
			throw new NetworkExportException("An error occurred loading the network to NDEx.");
		} finally {

			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// Revert names back to original?
			rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, collectionName);
			network.getRow(network).set(CyNetwork.NAME, networkName);

			NetworkManager.INSTANCE.addNetworkUUID(suid, networkUUID);
			CXInfoHolder cxInfo = NetworkManager.INSTANCE.getCXInfoHolder(suid);
			if (cxInfo != null)
				cxInfo.setNetworkId(networkUUID);
			CyObjectManager.INSTANCE.getApplicationFrame().revalidate();
		}

		if (networkUUID == null) {
			throw new NetworkExportException("There was a problem exporting the network!");
		}

	}

	private void prepareForUpdate(long suid) throws NetworkUpdateException {
		CXInfoHolder cxInfo = NetworkManager.INSTANCE.getCXInfoHolder(suid);
		if (cxInfo != null)
			networkUUID = cxInfo.getNetworkId();
		if (networkUUID == null)
			networkUUID = NetworkManager.INSTANCE.getNdexNetworkId(suid);
		if (networkUUID == null) {
			throw new NetworkUpdateException(
					"NDEx network UUID not found. You can only update networks that were imported with CyNDEx2");
		}
	}

	public UUID getNetworkUUID() {
		return networkUUID;
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

}
