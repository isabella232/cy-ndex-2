package org.cytoscape.cyndex2.internal.ui.swing;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JPopupMenu;

import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.ServerManager;

public class SignInButtonHelper {

    public static JButton createSignInButton() {
        //Create the popup menu.
        final JPopupMenu popup = new ProfilePopupMenu();
        JButton signInButton = new JButton();
        
        signInButton.setText(getSignInText());
       
        signInButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        return signInButton;
    }

    public static String getSignInText() {
    	final Server selectedServer = ServerManager.INSTANCE.getSelectedServer();
    	
    	if (selectedServer == null) {
    		return "Sign In";
    	} else {
    		return selectedServer.getUsername() + "@" + selectedServer.getUrl();
    	}
    }
}
