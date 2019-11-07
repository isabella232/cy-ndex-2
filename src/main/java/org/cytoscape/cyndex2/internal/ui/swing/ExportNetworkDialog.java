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

package org.cytoscape.cyndex2.internal.ui.swing;

import java.awt.Frame;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.ws.rs.core.Response;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.cytoscape.cyndex2.external.SaveParameters;
import org.cytoscape.cyndex2.internal.rest.SimpleNetworkSummary;
import org.cytoscape.cyndex2.internal.rest.parameter.LoadParameters;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExImportParameters;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExSaveParameters;
import org.cytoscape.cyndex2.internal.rest.response.SummaryResponse;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.ServerManager;
import org.cytoscape.group.CyGroupManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;

import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.Permissions;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author David
 */
public class ExportNetworkDialog extends javax.swing.JDialog {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	final SaveParameters saveParameters;
	
	/**
	 * Creates new form UploadNetwork
	 */
	public ExportNetworkDialog(Frame parent, SaveParameters saveParameters) {
		super(parent, false);
		this.saveParameters = saveParameters;
		initComponents();
		prepComponents();
	}

	private String getCurrentNetworkName(SummaryResponse summaryResponse) {
		for (SimpleNetworkSummary networkSummary : summaryResponse.members) {
				if (networkSummary.suid == summaryResponse.currentNetworkSuid) {
					return networkSummary.name;
				}
		}
		return null;
	}

	private void prepComponents()
    {
        //setModal(true);
        rootPane.setDefaultButton(upload);
       
    		String REST_URI = "http://localhost:1234/cyndex2/v1/networks/current";
				HttpClient httpClient = HttpClients.createDefault();
				final URI uri = URI.create(REST_URI);
				final HttpGet get = new HttpGet(uri.toString());
				get.setHeader("Content-type", "application/json");
				HttpResponse response;
				try {
					response = httpClient.execute(get);
				
					final HttpEntity entity = response.getEntity();
					final String result = EntityUtils.toString(entity);
					
					ObjectMapper objectMapper = new ObjectMapper();
					JsonNode jsonNode = objectMapper.readValue(result, JsonNode.class);
					System.out.println(jsonNode);
					
					SummaryResponse summaryResponse = objectMapper.treeToValue(jsonNode.get("data"), SummaryResponse.class);
					System.out.println(summaryResponse);
				
       // CyNetwork cyNetwork = CyObjectManager.INSTANCE.getCurrentNetwork();
        boolean updatePossible = false; //updateIsPossible();
        updateCheckbox.setSelected(false);
        if( !updatePossible )
            updateCheckbox.setEnabled(false);
        
        //String networkName = cyNetwork.getRow(cyNetwork).get(CyNetwork.NAME, String.class);
        nameField.setText(getCurrentNetworkName(summaryResponse));
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
        //Server selectedServer = ServerManager.INSTANCE.getSelectedServer();
       
    }

				/*
	private boolean updateIsPossible() {
		return updateIsPossibleHelper() != null;
	}

	private NetworkSummary updateIsPossibleHelper() {
		CyNetwork cyNetwork = CyObjectManager.INSTANCE.getCurrentNetwork();

		UUID ndexNetworkId = null;
		CXInfoHolder cxInfo = NetworkManager.INSTANCE.getCXInfoHolder(cyNetwork.getSUID());
		if (cxInfo != null) {
			ndexNetworkId = cxInfo.getNetworkId();
		} else {
			ndexNetworkId = NetworkManager.INSTANCE.getNdexNetworkId(cyNetwork.getSUID());
		}

		if (ndexNetworkId == null)
			return null;

		Server selectedServer = ServerManager.INSTANCE.getSelectedServer();
		NdexRestClientModelAccessLayer mal = selectedServer.getModelAccessLayer();
		UUID userId = ServerManager.INSTANCE.getSelectedServer().getUserId();

		try {
			Map<String, Permissions> permissionTable = mal.getUserNetworkPermission(userId, ndexNetworkId, false);
			if (permissionTable == null || permissionTable.get(ndexNetworkId.toString()) == Permissions.READ)
				return null;

		} catch (IOException e) {
			return null;
		}

		NetworkSummary ns = null;
		try {
			ns = mal.getNetworkSummaryById(ndexNetworkId.toString());
			if (ns.getIsReadOnly())
				return null;

		} catch (IOException e) {
			return null;
		} catch (NdexException e) {
			return null;
		}
		return ns;
	}
	*/
	/*
	 * private void updateModificationTimeLocally() { CyNetwork cyNetwork =
	 * CyObjectManager.INSTANCE.getCurrentNetwork(); CyRootNetwork rootNetwork =
	 * ((CySubNetwork)cyNetwork).getRootNetwork(); CyRow r =
	 * rootNetwork.getRow(rootNetwork); String modificationTime =
	 * r.get("ndex:modificationTime", String.class); String networkId =
	 * r.get("ndex:uuid", String.class); if( modificationTime == null || networkId
	 * == null ) return; Server selectedServer =
	 * ServerManager.INSTANCE.getSelectedServer(); NdexRestClientModelAccessLayer
	 * mal = selectedServer.getModelAccessLayer(); NetworkSummary ns = null; try {
	 * ns = mal.getNetworkSummaryById(networkId); } catch (IOException |
	 * NdexException e) { return; } r.set("ndex:modificationTime",
	 * ns.getModificationTime().toString()); }
	 */

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButton1 = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        nameField = new javax.swing.JTextField();
        upload = new javax.swing.JButton();
        cancel = new javax.swing.JButton();
        updateCheckbox = new javax.swing.JCheckBox();
        jButton2 = SignInButtonHelper.createSignInButton();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        authorField = new javax.swing.JTextField();
        organismField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        diseaseField = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        tissueField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        rightsHolderField = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        versionField = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        referenceField = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        descriptionTextArea = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Upload Network to NDEx");

        jLabel5.setText("Name:");

        nameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nameFieldActionPerformed(evt);
            }
        });

        upload.setText("Export Network To NDEx");
        upload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                uploadActionPerformed(evt);
            }
        });

        cancel.setText("Cancel");
        cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelActionPerformed(evt);
            }
        });

        updateCheckbox.setText("Update Existing Network");
        updateCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateCheckboxActionPerformed(evt);
            }
        });

        jButton2.setText("profile");

        jLabel6.setText("Author");

        jLabel7.setText("Organism");

        organismField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                organismFieldActionPerformed(evt);
            }
        });

        jLabel8.setText("Disease");

        jLabel9.setText("Tissue");

        jLabel10.setText("Rights Holder");

        jLabel11.setText("Version");

        jLabel12.setText("Reference");

        referenceField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                referenceFieldActionPerformed(evt);
            }
        });

        jLabel13.setText("Description");

        descriptionTextArea.setColumns(20);
        descriptionTextArea.setRows(5);
        jScrollPane1.setViewportView(descriptionTextArea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7)
                            .addComponent(jLabel6)
                            .addComponent(jLabel5)
                            .addComponent(jLabel8)
                            .addComponent(jLabel9)
                            .addComponent(jLabel10)
                            .addComponent(jLabel11)
                            .addComponent(jLabel12)
                            .addComponent(jLabel13))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(organismField)
                            .addComponent(diseaseField)
                            .addComponent(tissueField)
                            .addComponent(rightsHolderField)
                            .addComponent(versionField)
                            .addComponent(jScrollPane1)
                            .addComponent(referenceField, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(nameField)
                            .addComponent(authorField)))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(cancel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(upload))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButton2)))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(218, 218, 218)
                .addComponent(updateCheckbox)
                .addContainerGap(223, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButton2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(nameField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(authorField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(organismField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(diseaseField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(tissueField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(rightsHolderField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(versionField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(referenceField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel13)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(updateCheckbox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancel)
                    .addComponent(upload))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void nameFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nameFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_nameFieldActionPerformed

    private void organismFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_organismFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_organismFieldActionPerformed

    private void referenceFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_referenceFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_referenceFieldActionPerformed

	private void prepareToWriteNetworkToCXStream(CyNetwork cyNetwork, PipedOutputStream out, boolean isUpdate) {
	/*
		VisualMappingManager vmm = CyObjectManager.INSTANCE.getVisualMappingManager();
		final CyNetworkViewManager nvm = CyObjectManager.INSTANCE.getNetworkViewManager();
		final CyGroupManager gm = CyObjectManager.INSTANCE.getCyGroupManager();
		final VisualLexicon lexicon = CyObjectManager.INSTANCE.getDefaultVisualLexicon();
		CxNetworkWriter writer = new CxNetworkWriter(out, cyNetwork, vmm, nvm, gm, lexicon, isUpdate);
		boolean writeEntireCollection = networkOrCollectionCombo.getSelectedIndex() == 1;
		writer.setWriteSiblings(writeEntireCollection);
		TaskIterator ti = new TaskIterator(writer);
		TaskManager tm = CyObjectManager.INSTANCE.getTaskManager();
		tm.execute(ti);
		*/
	}

	private void uploadActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_uploadActionPerformed
		final JTextField nameField = this.nameField;
		
		SwingWorker<Integer, Integer> worker = new SwingWorker<Integer, Integer>() {
			
			@Override
			protected Integer doInBackground() throws Exception {

	
		
		final Server selectedServer = ServerManager.INSTANCE.getSelectedServer();
		
		final String REST_URI = "http://localhost:1234/cyndex2/v1/networks/" + saveParameters.suid;

		HttpClient httpClient = HttpClients.createDefault();
		final URI uri = URI.create(REST_URI);
		final HttpPost post = new HttpPost(uri.toString());
		post.setHeader("Content-type", "application/json");

		Map<String, String> metadata = Map.of(
				"name",nameField.getText());
		System.out.println("REST_URI: " + REST_URI);
		System.out.println("url " + selectedServer.getUrl());
		
		NDExSaveParameters saveParameters = new NDExSaveParameters(
				selectedServer.getUsername(), 
				selectedServer.getPassword(), 
				selectedServer.getUrl(), 
				metadata, 
				false);
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			post.setEntity(new StringEntity(objectMapper.writeValueAsString(saveParameters)));
			HttpResponse response = httpClient.execute(post);
			
			System.out.println("Response: " + response.toString());
			
			
		}
		catch (UnsupportedEncodingException | JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				return 1;
			}
			};
			worker.execute();
		/*
		CyNetwork cyNetwork = CyObjectManager.INSTANCE.getCurrentNetwork(); // get the current subNetwork

		if (cyNetwork.getEdgeCount() > 10000) {
			JFrame parent = CyObjectManager.INSTANCE.getApplicationFrame();
			String msg = "You have chosen to upload a network that has more than 10,000 edges.\n";
			msg += "The upload will occur in the background and you can continue working,\n";
			msg += "but it may take a while to appear in NDEx. Would you like to proceed?";
			String dialogTitle = "Proceed?";
			int choice = JOptionPane.showConfirmDialog(parent, msg, dialogTitle, JOptionPane.YES_NO_OPTION);
			if (choice == JOptionPane.NO_OPTION)
				return;
		}

		CyRootNetwork rootNetwork = ((CySubNetwork) cyNetwork).getRootNetwork();

		
		String collectionName = rootNetwork.getRow(rootNetwork).get(CyNetwork.NAME, String.class);
		String uploadName = nameField.getText().trim();
		String networkName = cyNetwork.getRow(cyNetwork).get(CyNetwork.NAME, String.class);

		rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, uploadName);
		// If network is selected
		if (networkOrCollectionCombo.getSelectedIndex() == 0)
			cyNetwork.getRow(cyNetwork).set(CyNetwork.NAME, uploadName);

		Server selectedServer = ServerManager.INSTANCE.getSelectedServer();
		final NdexRestClientModelAccessLayer mal = selectedServer.getModelAccessLayer();

		PipedInputStream in = null;
		PipedOutputStream out = null;

		UUID networkUUID = null;
		// boolean networkUpdated = false;
		try {
			in = new PipedInputStream();
			out = new PipedOutputStream(in);

			if (updateCheckbox.isSelected()) {
				UUID networkId = NetworkManager.INSTANCE.getNdexNetworkId(cyNetwork.getSUID());
				if (networkId == null)
					networkId = NetworkManager.INSTANCE.getCXInfoHolder(cyNetwork.getSUID()).getNetworkId();

				if (updateIsPossible()) {
					
					prepareToWriteNetworkToCXStream(cyNetwork, out, true);
					mal.updateCXNetwork(networkId, in);
					networkUUID = networkId;
					
				} else {
					JFrame parent = CyObjectManager.INSTANCE.getApplicationFrame();
					String msg = "You have chosen to update, but it is no longer possible.\n";
					msg += "Would you like to proceed by creating a new network instead?\n";
					String dialogTitle = "Proceed?";
					int choice = JOptionPane.showConfirmDialog(parent, msg, dialogTitle, JOptionPane.YES_NO_OPTION);
					if (choice == JOptionPane.NO_OPTION)
						return;
					prepareToWriteNetworkToCXStream(cyNetwork, out, false);
					networkUUID = mal.createCXNetwork(in);
				}

			} else {
				prepareToWriteNetworkToCXStream(cyNetwork, out, false);
				networkUUID = mal.createCXNetwork(in);
			}

		} catch (Exception e) {
			e.printStackTrace();
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
			rootNetwork.getRow(rootNetwork).set(CyNetwork.NAME, collectionName);
			cyNetwork.getRow(cyNetwork).set(CyNetwork.NAME, networkName);
			
			CyObjectManager.INSTANCE.getApplicationFrame().revalidate();
		}

		if (networkUUID == null) {
			JFrame parent = CyObjectManager.INSTANCE.getApplicationFrame();
			String msg = "There was a problem exporting the network!";
			JOptionPane.showMessageDialog(parent, msg, "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground() throws Exception {
				return null;
			}
		};
		worker.execute();
*/
		this.setVisible(false);
		
	}// GEN-LAST:event_uploadActionPerformed

	private void cancelActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_cancelActionPerformed
	{// GEN-HEADEREND:event_cancelActionPerformed
		this.setVisible(false);
	}// GEN-LAST:event_cancelActionPerformed

	private void updateCheckboxActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_updateCheckboxActionPerformed
	{// GEN-HEADEREND:event_updateCheckboxActionPerformed
		// TODO add your handling code here:
		System.out.println("update checked.");
	}// GEN-LAST:event_updateCheckboxActionPerformed

	/**
	 * @param args the command line arguments
	 */
	/*
	 * public static void main(String args[]) { // Set the Nimbus look and feel
	 * //<editor-fold defaultstate="collapsed"
	 * desc=" Look and feel setting code (optional) "> // If Nimbus (introduced in
	 * Java SE 6) is not available, stay with the default look and feel. // For
	 * details see
	 * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
	 * 
	 * try { for (javax.swing.UIManager.LookAndFeelInfo info :
	 * javax.swing.UIManager.getInstalledLookAndFeels()) { if
	 * ("Nimbus".equals(info.getName())) {
	 * javax.swing.UIManager.setLookAndFeel(info.getClassName()); break; } } } catch
	 * (ClassNotFoundException ex) {
	 * java.util.logging.Logger.getLogger(ExportNetworkDialog.class.getName()).log(
	 * java.util.logging.Level.SEVERE, null, ex); } catch (InstantiationException
	 * ex) {
	 * java.util.logging.Logger.getLogger(ExportNetworkDialog.class.getName()).log(
	 * java.util.logging.Level.SEVERE, null, ex); } catch (IllegalAccessException
	 * ex) {
	 * java.util.logging.Logger.getLogger(ExportNetworkDialog.class.getName()).log(
	 * java.util.logging.Level.SEVERE, null, ex); } catch
	 * (javax.swing.UnsupportedLookAndFeelException ex) {
	 * java.util.logging.Logger.getLogger(ExportNetworkDialog.class.getName()).log(
	 * java.util.logging.Level.SEVERE, null, ex); } //</editor-fold>
	 * //</editor-fold>
	 * 
	 * // Create and display the dialog java.awt.EventQueue.invokeLater(new
	 * Runnable() { public void run() { ExportNetworkDialog dialog = new
	 * ExportNetworkDialog(new javax.swing.JFrame()); dialog.addWindowListener(new
	 * java.awt.event.WindowAdapter() {
	 * 
	 * @Override public void windowClosing(java.awt.event.WindowEvent e) {
	 * System.exit(0); } }); dialog.setVisible(true); } }); }
	 */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField authorField;
    private javax.swing.JButton cancel;
    private javax.swing.JTextArea descriptionTextArea;
    private javax.swing.JTextField diseaseField;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField nameField;
    private javax.swing.JTextField organismField;
    private javax.swing.JTextField referenceField;
    private javax.swing.JTextField rightsHolderField;
    private javax.swing.JTextField tissueField;
    private javax.swing.JCheckBox updateCheckbox;
    private javax.swing.JButton upload;
    private javax.swing.JTextField versionField;
    // End of variables declaration//GEN-END:variables

}
