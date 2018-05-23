package org.cytoscape.cyndex2.internal.ui;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.AbstractToolBarComponent;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;

public class SaveNetworkToNDExToolbarComponent extends AbstractToolBarComponent implements SetCurrentNetworkListener {

    private JButton button;

	public SaveNetworkToNDExToolbarComponent() {
		super();
		button = new JButton();
		setToolBarGravity(1.8f);

		ImageIcon icon = new ImageIcon(this.getClass().getClassLoader().getResource("images/ndex-cy-export-72x72.png"));
		icon = new ImageIcon(icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH));
		button.setIcon(icon);
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setContentAreaFilled(true);
		button.setToolTipText("Export Network to NDEx");
		button.setEnabled(false);
		
		button.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				ExternalAppManager.saveType = ExternalAppManager.SAVE_NETWORK;
				ExternalAppManager.query = "";
				CyActivator.openBrowserDialog(ExternalAppManager.APP_NAME_SAVE);
			}
		});
	}		

	@Override
	public Component getComponent() {
		return button;
	}

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        button.setEnabled(e.getNetwork() != null);
    }
}