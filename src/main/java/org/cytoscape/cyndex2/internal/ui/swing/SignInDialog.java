/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cytoscape.cyndex2.internal.ui.swing;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.cyndex2.internal.util.ServerManager;
import org.cytoscape.util.swing.IconManager;

import static org.cytoscape.util.swing.IconManager.ICON_COG;

/**
 *
 * @author dotasek
 */
public class SignInDialog extends javax.swing.JDialog {

	final int ICON_FONT_SIZE = 22;

	private String serverURL = "public.ndexbio.org";

	/**
	 * Creates new form NewJDialog
	 */
	public SignInDialog(JDialog parent) {
		super(parent, true);
		initComponents();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
	// <editor-fold defaultstate="collapsed" desc="Generated
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		jLabel2 = new javax.swing.JLabel();
		username = new javax.swing.JTextField();
		jLabel5 = new javax.swing.JLabel();
		jLabel6 = new javax.swing.JLabel();
		save = new javax.swing.JButton();
		cancel = new javax.swing.JButton();
		password = new javax.swing.JPasswordField();
		forgotPasswordLabel = new javax.swing.JLabel();
		needAccountLabel = new javax.swing.JLabel();
		signUpLabel = new javax.swing.JLabel();
		updateSettingsButton = new JButton(ICON_COG);

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

		jLabel2.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
		jLabel2.setText("NDEx Sign in");

		jLabel5.setText("Username:");

		jLabel6.setText("Password:");

		save.setText("Sign in");
		save.setMaximumSize(new java.awt.Dimension(63, 24));
		save.setMinimumSize(new java.awt.Dimension(63, 24));
		save.setPreferredSize(new java.awt.Dimension(63, 24));
		save.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				saveActionPerformed(evt);
			}
		});

		cancel.setText("Cancel");
		cancel.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cancelActionPerformed(evt);
			}
		});

		forgotPasswordLabel.setForeground(new java.awt.Color(51, 122, 183));
		forgotPasswordLabel.setText("Forgot your password?");
		forgotPasswordLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		forgotPasswordLabel.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				forgotPasswordLabelMouseClicked(evt);
			}
		});

		needAccountLabel.setText("Need an account?");

		signUpLabel.setForeground(new java.awt.Color(51, 122, 183));
		signUpLabel.setText("Click here to Sign Up!");
		signUpLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		signUpLabel.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				signUpLabelMouseClicked(evt);
			}
		});

		updateSettingsButton.setFont(CyServiceModule.getService(IconManager.class).getIconFont(ICON_FONT_SIZE * 4 / 5));
		updateSettingsButton.setToolTipText("Additional sign-in options");
		updateSettingsButton.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
		updateSettingsButton.setBorderPainted(false);
		updateSettingsButton.setContentAreaFilled(false);
		updateSettingsButton.setFocusPainted(false);
		updateSettingsButton.setMaximumSize(new java.awt.Dimension(32, 32));
		updateSettingsButton.setMinimumSize(new java.awt.Dimension(24, 24));
		updateSettingsButton.setPreferredSize(new java.awt.Dimension(24, 24));
		updateSettingsButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				updateSettingsButtonActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(layout.createSequentialGroup()
										.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(jLabel5).addComponent(jLabel6))
										.addGap(29, 29, 29).addGroup(
												layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(username).addComponent(password)))
								.addGroup(layout.createSequentialGroup()
										.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
												.addComponent(jLabel2).addComponent(forgotPasswordLabel)
												.addGroup(layout.createSequentialGroup().addComponent(needAccountLabel)
														.addPreferredGap(
																javax.swing.LayoutStyle.ComponentPlacement.RELATED)
														.addComponent(signUpLabel)))
										.addGap(0, 0, Short.MAX_VALUE))
								.addGroup(javax.swing.GroupLayout.Alignment.TRAILING,
										layout.createSequentialGroup().addGap(0, 137, Short.MAX_VALUE)
												.addComponent(save, javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addGap(69, 69, 69))
								.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
										.addComponent(updateSettingsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 24,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
												javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addComponent(cancel)))
						.addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jLabel2)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(username, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jLabel5))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel6)
								.addComponent(password, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(layout
								.createSequentialGroup()
								.addComponent(save, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(forgotPasswordLabel)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(needAccountLabel).addComponent(signUpLabel))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
								.addComponent(cancel, javax.swing.GroupLayout.PREFERRED_SIZE, 24,
										javax.swing.GroupLayout.PREFERRED_SIZE))
								.addGroup(layout.createSequentialGroup().addGap(0, 0, Short.MAX_VALUE).addComponent(
										updateSettingsButton, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
						.addContainerGap()));

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void updateSettingsButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_updateSettingsButtonActionPerformed
		SignInAdvancedDialog advancedSettingsDialog = new SignInAdvancedDialog(this, serverURL);
		advancedSettingsDialog.setLocationRelativeTo(this);
		advancedSettingsDialog.setVisible(true);

		if (advancedSettingsDialog.isChanged()) {
			serverURL = advancedSettingsDialog.getNewServerURL();
		}
		advancedSettingsDialog.dispose();
	}// GEN-LAST:event_updateSettingsButtonActionPerformed

	private void saveActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_saveActionPerformed
		signInToServer();
	}// GEN-LAST:event_saveActionPerformed

	private void cancelActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cancelActionPerformed
		setVisible(false);
	}// GEN-LAST:event_cancelActionPerformed


	private void forgotPasswordLabelMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_forgotPasswordLabelMouseClicked

		try {
			AccountBrowserPrompt dialog;
			dialog = new AccountBrowserPrompt(null, true, "Password Recovery",
					new URI(ServerManager.addHttpsProtocol(serverURL) + "/viewer/recoverPassword"));
			dialog.setLocationRelativeTo(this);
			dialog.setVisible(true);
		} catch (URISyntaxException ex) {
			Logger.getLogger(SignInDialog.class.getName()).log(Level.SEVERE, null, ex);
		}

	}// GEN-LAST:event_forgotPasswordLabelMouseClicked

	private void signUpLabelMouseClicked(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_signUpLabelMouseClicked

		try {
			AccountBrowserPrompt dialog;
			dialog = new AccountBrowserPrompt(null, true, "Sign-Up", new URI(ServerManager.addHttpsProtocol(serverURL) + "/viewer/signup"));
			dialog.setLocationRelativeTo(this);
			dialog.setVisible(true);
		} catch (URISyntaxException ex) {
			Logger.getLogger(SignInDialog.class.getName()).log(Level.SEVERE, null, ex);
		}

	}// GEN-LAST:event_signUpLabelMouseClicked

	private void signInToServer() {
		final String usernameText = this.username.getText();
		final String passwordText = new String(this.password.getPassword());

		final String username = usernameText.trim().length() > 0 ? usernameText.trim() : null;
		final String password = passwordText.length() > 0 ? passwordText : null;

		final String serverUrl = serverURL;

		try {
			ServerManager.INSTANCE.addServer(username, password, serverUrl);
			setVisible(false);
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, ex.getMessage(), "Error Adding Server", JOptionPane.ERROR_MESSAGE);
		}
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
			java.util.logging.Logger.getLogger(SignInDialog.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(SignInDialog.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(SignInDialog.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(SignInDialog.class.getName()).log(java.util.logging.Level.SEVERE, null,
					ex);
		}
		// </editor-fold>
		// </editor-fold>

		/* Create and display the dialog */
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				SignInDialog dialog = new SignInDialog(new javax.swing.JDialog());
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
	private javax.swing.JButton cancel;
	private javax.swing.JLabel forgotPasswordLabel;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel needAccountLabel;
	private javax.swing.JPasswordField password;
	private javax.swing.JButton save;
	private javax.swing.JLabel signUpLabel;
	private javax.swing.JButton updateSettingsButton;
	private javax.swing.JTextField username;
	// End of variables declaration//GEN-END:variables
}
