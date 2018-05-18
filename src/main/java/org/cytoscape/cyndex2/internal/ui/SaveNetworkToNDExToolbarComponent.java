package org.cytoscape.cyndex2.internal.ui;

import java.awt.Component;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.AbstractToolBarComponent;

/**
 * Toolbar component on Cytoscape toolbar to post and update graphs to GraphSpace
 * @author rishabh
 *
 */
public class SaveNetworkToNDExToolbarComponent extends AbstractToolBarComponent implements SetCurrentNetworkListener {

    private JButton button;

	public SaveNetworkToNDExToolbarComponent() {
		super();
		button = new JButton();
		setToolBarGravity(1.8f);

		//imageicon used as for the toolbar menu
		ImageIcon icon = new ImageIcon(this.getClass().getClassLoader().getResource("images/ndex-logo.png"));
		icon = new ImageIcon(icon.getImage().getScaledInstance(48, 35, Image.SCALE_SMOOTH)); // resize image
		button.setIcon(icon);
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setContentAreaFilled(true);
		button.setToolTipText("Export To GraphSpace"); //set tooltip to notify users about the functionality of the button
		button.setEnabled(false);
		
		//action attached to the button
//		actionListener = new PostGraphMenuActionListener();
//		button.addActionListener(actionListener);
	}
		
	/** Returns an ImageIcon, or null if the path was invalid. */

	@Override
	public Component getComponent() {
		//returns the button component to be visible on the toolbar
		return button;
	}

    @Override
    public void handleEvent(SetCurrentNetworkEvent e) {
        button.setEnabled(e.getNetwork() != null);
    }
}