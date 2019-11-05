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

import java.awt.Component;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.cytoscape.cyndex2.internal.rest.parameter.LoadParameters;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExImportParameters;
import org.cytoscape.cyndex2.internal.util.ErrorMessage;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.ServerManager;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.NdexPropertyValuePair;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author David
 * @author David Otasek
 */
public class FindNetworksDialog extends javax.swing.JDialog {
	
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;
	private List<NetworkSummary> networkSummaries;

	/**
	 * Creates new form SimpleSearch
	 */
	public FindNetworksDialog(Frame parent, LoadParameters loadParameters) {
		super(parent, false);
		initComponents();
		prepComponents(loadParameters.searchTerm);
	}

	public void setFocusOnDone() {
		this.getRootPane().setDefaultButton(done);
		done.requestFocus();
	}

	private void prepComponents(String searchTerm) {
		this.setModal(false);
		this.getRootPane().setDefaultButton(search);

		Server selectedServer = ServerManager.INSTANCE.getSelectedServer();
	
		searchField.setText(searchTerm);

		if (selectedServer.isAuthenticated()) {
			administeredByMe.setVisible(true);
		} else {
			if (selectedServer.getUsername() != null) {
				NdexRestClientModelAccessLayer mal = selectedServer.getModelAccessLayer();
				try {
					selectedServer.check(mal);
					administeredByMe.setVisible(true);
				} catch (IOException e) {
					e.printStackTrace();
					JOptionPane.showMessageDialog(this, ErrorMessage.failedServerCommunication + ": " + e.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
					this.setVisible(false);
					return;
				}
			} else {
				administeredByMe.setVisible(false);
			}
		}

		NdexRestClientModelAccessLayer mal = selectedServer.getModelAccessLayer();
		try {
			if (selectedServer.check(mal)) {
				try {
					networkSummaries = mal.findNetworks(searchTerm, null, null, true, 0, 400).getNetworks();
				} catch (IOException | NdexException ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(this, ErrorMessage.failedServerCommunication, "Error",
							JOptionPane.ERROR_MESSAGE);
					this.setVisible(false);
					return;
				}
				showSearchResults();
			} else {
				JOptionPane.showMessageDialog(this, ErrorMessage.failedServerCommunication, "Error", JOptionPane.ERROR_MESSAGE);
				this.setVisible(false);
			}
		} catch (HeadlessException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, ErrorMessage.failedServerCommunication, "Error", JOptionPane.ERROR_MESSAGE);

		}
	}

	private void load(final NetworkSummary networkSummary) {
		// Note: In this code, references named network, node, and edge generally refer
		// to the NDEx object model
		// while references named cyNetwork, cyNode, and cyEdge generally refer to the
		// Cytoscape object model.

		boolean largeNetwork = false;

		largeNetwork = networkSummary.getEdgeCount() > 10000;

		if (largeNetwork) {
			/*
			 * JFrame parent = CyObjectManager.INSTANCE.getApplicationFrame(); String msg =
			 * "You have chosen to download a network that has more than 10,000 edges.\n";
			 * msg +=
			 * "The download will occur in the background and you can continue working,\n";
			 * msg +=
			 * "but it may take a while to appear in Cytoscape. Also, no layout will be\n";
			 * msg += "applied. Would you like to proceed?"; String dialogTitle =
			 * "Proceed?"; int choice = JOptionPane.showConfirmDialog(parent, msg,
			 * dialogTitle, JOptionPane.YES_NO_OPTION); if (choice == JOptionPane.NO_OPTION)
			 * return;
			 */
		}
//        final boolean finalLargeNetwork = largeNetwork;

		final Component me = this;
		// final boolean isLargeNetwork = largeNetwork;
		SwingWorker<Integer, Integer> worker = new SwingWorker<Integer, Integer>() {

			@Override
			protected Integer doInBackground() throws Exception {

				{
					// For entire network, we will query again, hence will check credential
					final Server selectedServer = ServerManager.INSTANCE.getSelectedServer();
					final NdexRestClientModelAccessLayer mal = selectedServer.getModelAccessLayer();
					boolean success = selectedServer.check(mal);
					if (success) {
						// The network to copy from.
//                        NetworkSummary networkSummary = NetworkManager.INSTANCE.getSelectedNetworkSummary();
						UUID uuid = networkSummary.getExternalId();
						try {
							// ProvenanceEntity provenance = mal.getNetworkProvenance(id.toString());

							String REST_URI = "http://localhost:1234/cyndex2/v1/networks";
							HttpClient httpClient = HttpClients.createDefault();
							final URI uri = URI.create(REST_URI);
							final HttpPost post = new HttpPost(uri.toString());
							post.setHeader("Content-type", "application/json");

							NDExImportParameters importParameters = new NDExImportParameters(uuid.toString(), null, null,
									selectedServer.getUrl(), null, null);
							ObjectMapper objectMapper = new ObjectMapper();

							post.setEntity(new StringEntity(objectMapper.writeValueAsString(importParameters)));

							httpClient.execute(post);

						}

						/*
						 * catch (IOException ex) { JOptionPane.showMessageDialog(me,
						 * ErrorMessage.failedToParseJson, "Error", JOptionPane.ERROR_MESSAGE); return
						 * -1; }
						 */
						catch (RuntimeException ex2) {
							JOptionPane.showMessageDialog(me,
									"This network can't be imported to cytoscape. Cause: " + ex2.getMessage(), "Error",
									JOptionPane.ERROR_MESSAGE);
							ex2.printStackTrace();
							return -1;
						}
					} else {
						JOptionPane.showMessageDialog(me, ErrorMessage.failedServerCommunication, "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
				return 1;
			}

		};
		worker.execute();
//        findNetworksDialog.setFocusOnDone();
//        this.setVisible(false);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        resultsTable = new javax.swing.JTable();
        selectNetwork = new javax.swing.JButton();
        done = new javax.swing.JButton();
        search = new javax.swing.JButton();
        searchField = new javax.swing.JTextField();
        administeredByMe = new javax.swing.JCheckBox();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jButton1 = SignInButtonHelper.createSignInButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Find Networks");
        setPreferredSize(new java.awt.Dimension(600, 525));

        resultsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null}
            },
            new String [] {
                "Network Title", "Format", "Number of Nodes", "Number of Edges", "Owned By", "Last Modified"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        resultsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(resultsTable);

        selectNetwork.setText("Load Network");
        selectNetwork.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectNetworkActionPerformed(evt);
            }
        });

        done.setText("Done Loading Networks");
        done.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                doneActionPerformed();
            }
        });

        search.setText("Search");
        search.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchActionPerformed(evt);
            }
        });

        administeredByMe.setText("My Networks");
        administeredByMe.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                administeredByMeActionPerformed();
            }
        });

        jLabel1.setText("Results");

        jLabel4.setText("WARNING: In some cases, not all network information stored in NDEx will be available within Cytoscape after loading.");

        jButton1.setText("profile");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addComponent(jSeparator1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(searchField)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(search))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(selectNetwork, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(done, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 183, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton1, javax.swing.GroupLayout.Alignment.TRAILING))))
                .addContainerGap())
            .addGroup(layout.createSequentialGroup()
                .addGap(377, 377, 377)
                .addComponent(administeredByMe)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton1)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(searchField, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(search, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(administeredByMe)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(selectNetwork)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(done)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void selectNetworkActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_selectNetworkActionPerformed
	{// GEN-HEADEREND:event_selectNetworkActionPerformed
		int selectedIndex = resultsTable.getSelectedRow();
		if (selectedIndex == -1) {
			JOptionPane.showMessageDialog(this, ErrorMessage.noNetworkSelected, "Error", JOptionPane.ERROR_MESSAGE);
		}
		NetworkSummary ns = displayedNetworkSummaries.get(selectedIndex);
		// NetworkManager.INSTANCE.setSelectedNetworkSummary(ns);

		load(ns);
	}// GEN-LAST:event_selectNetworkActionPerformed

	private void getMyNetworks() {
		Server selectedServer = ServerManager.INSTANCE.getSelectedServer();

		NdexRestClientModelAccessLayer mal = selectedServer.getModelAccessLayer();
		try {
			if (selectedServer.check(mal)) {
				/*
				 * try { networkSummaries = mal.getMyNetworks(selectedServer.getUserId()); }
				 * catch (IOException ex) { ex.printStackTrace();
				 * JOptionPane.showMessageDialog(this, ex.getMessage(), "Error",
				 * JOptionPane.ERROR_MESSAGE); return; }
				 */
				showSearchResults();
			} else {
				JOptionPane.showMessageDialog(this, ErrorMessage.failedServerCommunication, "ErrorY",
						JOptionPane.ERROR_MESSAGE);
				this.setVisible(false);
			}
		} catch (HeadlessException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, ErrorMessage.failedServerCommunication, "ErrorY", JOptionPane.ERROR_MESSAGE);

		}
	}

	private void doneActionPerformed() {
		
	}
	
	private void search() {
		Server selectedServer = ServerManager.INSTANCE.getSelectedServer();

		/*
		 * if( administeredByMe.isSelected() ) permissions = Permissions.READ;
		 */

		String searchText = searchField.getText();
		if (searchText.isEmpty())
			searchText = "";

		NdexRestClientModelAccessLayer mal = selectedServer.getModelAccessLayer();
		try {
			if (selectedServer.check(mal)) {
				try {
					if (administeredByMe.isSelected()) {
						// networkSummaries = mal.getMyNetworks(selectedServer.getUserId());
					} else
						networkSummaries = mal.findNetworks(searchText, null, null, true, 0, 10000).getNetworks();
				} catch (IOException | NdexException ex) {
					ex.printStackTrace();
					JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				showSearchResults();
			} else {
				JOptionPane.showMessageDialog(this, ErrorMessage.failedServerCommunication, "ErrorY",
						JOptionPane.ERROR_MESSAGE);
				this.setVisible(false);
			}
		} catch (HeadlessException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, ErrorMessage.failedServerCommunication, "ErrorY", JOptionPane.ERROR_MESSAGE);

		}
	}

	private void searchActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_searchActionPerformed
	{// GEN-HEADEREND:event_searchActionPerformed
		search();
	}
                                      

	private void administeredByMeActionPerformed()// GEN-FIRST:event_administeredByMeActionPerformed
	{// GEN-HEADEREND:event_administeredByMeActionPerformed
		getMyNetworks();
	}// GEN-LAST:event_administeredByMeActionPerformed

	private List<NetworkSummary> displayedNetworkSummaries = new ArrayList<>();

	private void showSearchResults() {
		DefaultTableModel model = new DefaultTableModel();
		model.setColumnIdentifiers(
				new String[] { "Network Title", "Format", "Number of Nodes", "Number of Edges", "Owned By", "Last Modified" });
		displayedNetworkSummaries.clear();
		for (NetworkSummary networkSummary : networkSummaries) {

			if (networkSummary.getErrorMessage() != null)
				continue;

			Vector row = new Vector();

			// Network Title
			if (networkSummary.getName() != null)
				row.add(networkSummary.getName());
			else {
				row.add("Network: " + networkSummary.getExternalId().toString());
			}
			// Format
			row.add(getSourceFormat(networkSummary));
			// Number of Nodes
			row.add(networkSummary.getNodeCount());
			// Number of Edges
			row.add(networkSummary.getEdgeCount());
			// Owned By
			row.add(networkSummary.getOwner());
			// Last Modified
			row.add(networkSummary.getModificationTime());

			model.addRow(row);
			displayedNetworkSummaries.add(networkSummary);
		}
		resultsTable.setModel(model);
		resultsTable.getSelectionModel().setSelectionInterval(0, 0);
	}

	private String getSourceFormat(NetworkSummary ns) {
		for (NdexPropertyValuePair vp : ns.getProperties()) {
			if (vp.getPredicateString().equals("ndex:sourceFormat"))
				return vp.getValue();
		}
		return null;
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {
		/* Set the Nimbus look and feel */
		// <editor-fold defaultstate="collapsed" desc=" Look and feel setting code
		// (optional) ">
		/*
		 * If Nimbus (introduced in Java SE 6) is not available, stay with the default
		 * look and feel. For details see
		 * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
		 */
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(FindNetworksDialog.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(FindNetworksDialog.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(FindNetworksDialog.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(FindNetworksDialog.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		}
		// </editor-fold>

		/* Create and display the dialog */
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				FindNetworksDialog dialog = new FindNetworksDialog(new javax.swing.JFrame(), null);
				dialog.addWindowListener(new java.awt.event.WindowAdapter() {
					@Override
					public void windowClosing(java.awt.event.WindowEvent e) {
						System.exit(0);
					}
				});
				dialog.setVisible(true);
			}
		});
	}

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox administeredByMe;
    private javax.swing.JButton done;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTable resultsTable;
    private javax.swing.JButton search;
    private javax.swing.JTextField searchField;
    private javax.swing.JButton selectNetwork;
    // End of variables declaration//GEN-END:variables
}
