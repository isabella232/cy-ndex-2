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
import java.util.UUID;

import javax.swing.SwingUtilities;

import org.cytoscape.cyndex2.internal.CxTaskFactoryManager;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.util.HeadlessTaskMonitor;
import org.cytoscape.cyndex2.internal.util.NetworkUUIDManager;
import org.cytoscape.io.read.AbstractCyNetworkReader;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

public class NetworkImportTask extends AbstractTask implements ObservableTask {

	final NdexRestClientModelAccessLayer mal;
	final NetworkSummary networkSummary;
	private UUID uuid = null;
	private Long suid = null;
	private String accessKey = null;
	protected InputStream cxStream;

	public NetworkImportTask(final NdexRestClientModelAccessLayer mal, UUID uuid, String accessKey)
			throws IOException, NdexException {
		super();
		this.uuid = uuid;
		/*
		
		*/
		this.mal = mal;
		networkSummary = mal.getNetworkSummaryById(uuid, accessKey);
		this.accessKey = accessKey;
		cxStream = null;
	}

	public NetworkImportTask(InputStream in) {
		super();
		networkSummary = null;
		mal = null;
		cxStream = in;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws NetworkImportException {

		// For entire network, we will query again, hence will check
		// credential
		// boolean success = true; // selectedServer.check(mal);
		// if (success) {
		try {
			taskMonitor.setStatusMessage("Fetching network from NDEx");
			if (cxStream == null) {
				UUID id = networkSummary.getExternalId();
				if (accessKey == null)
					cxStream = mal.getNetworkAsCXStream(id);
				else
					cxStream = mal.getNetworkAsCXStream(id, accessKey);
			}
			if (cxStream == null) {
				throw new NdexException("Unable to get network as CX stream");
			}
			taskMonitor.setProgress(.4);
			
			final InputStreamTaskFactory cxReaderFactory = 
					CxTaskFactoryManager.INSTANCE.getCxReaderFactory();
			
			taskMonitor.setStatusMessage("Importing network with CX Reader");
			TaskIterator ti = cxReaderFactory.createTaskIterator(cxStream, null);
			AbstractCyNetworkReader task = (AbstractCyNetworkReader) ti.next();
			SwingUtilities.invokeAndWait(new Runnable() {
				
				@Override
				public void run() {
					try {
						task.run(new HeadlessTaskMonitor());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			
			if (cancelled) {
				return;
			}
			
			taskMonitor.setProgress(.7);
			
			CyNetworkManager network_manager = CyServiceModule.getService(CyNetworkManager.class);
			int i = 1;
			for (CyNetwork network : task.getNetworks()) {
				if (cancelled) {
					return;
				}
				taskMonitor.setStatusMessage(String.format("Registering network %s/%s...", i, task.getNetworks().length));
				network_manager.addNetwork(network);
				task.buildCyNetworkView(network);
				i++;
			}
			taskMonitor.setProgress(.9);
			final CyNetwork network = task.getNetworks()[0];
			suid = network.getSUID();
			
			if (networkSummary.getSubnetworkIds().size() > 0) {	
				NetworkUUIDManager.saveUUID(((CySubNetwork)network).getRootNetwork(), uuid);
			} else {
				NetworkUUIDManager.saveUUID(network, uuid);
			}
			
		} catch (IOException ex) {
			throw new NetworkImportException("Failed to parse JSON from NDEx source.");
		} catch (RuntimeException ex2) {
			ex2.printStackTrace();
			throw new NetworkImportException(ex2.getMessage());
		} catch (NdexException e) {
			throw new NetworkImportException("Unable to read network from NDEx: " + e.getMessage());
		} catch(Exception e) {
			throw new RuntimeException("Failed to import: " + e.getMessage());
		}
	}
	
	@Override
	public void cancel() {
		super.cancel();
		try {
			cxStream.close();
		} catch (IOException e) {
			e.printStackTrace();
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

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getResults(Class<? extends R> type) {
		if (suid == null) {
			return null;
		}
		if (type.equals(Long.class)) {
			return (R) suid;
		}
		return null;
	}

	public long getSUID() {
		return suid;
	}

}
