package org.cytoscape.cyndex2.internal.ui;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.cytoscape.application.swing.AbstractToolBarComponent;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;

public class ImportNetworkFromNDExToolbarComponent extends AbstractToolBarComponent {

    private JButton button;

	public ImportNetworkFromNDExToolbarComponent() {
		super();
		button = new JButton();
		setToolBarGravity(2.8f);

		ImageIcon icon = new ImageIcon(this.getClass().getClassLoader().getResource("images/ndex-cy-import-72x72.png"));
		icon = new ImageIcon(icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH));
		button.setIcon(icon);
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setContentAreaFilled(true);
		button.setToolTipText("Import Network from NDEx");
		
		button.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				ExternalAppManager.query = "";
				CyActivator.openBrowserDialog(ExternalAppManager.APP_NAME_LOAD);
			}
		});
	}		

	@Override
	public Component getComponent() {
		return button;
	}

}