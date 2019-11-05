package org.cytoscape.cyndex2.internal.ui.swing;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JPopupMenu;

public class SignInButtonHelper {

	public static JButton createSignInButton() {
	//Create the popup menu.
    final JPopupMenu popup = new ProfilePopupMenu();
    
		JButton signInButton = new JButton("Profile");
		signInButton.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
          popup.show(e.getComponent(), e.getX(), e.getY());
      }
  });
		return signInButton;
	}
	

	
}
