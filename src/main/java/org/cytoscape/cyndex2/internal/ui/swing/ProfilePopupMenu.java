package org.cytoscape.cyndex2.internal.ui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.ServerList;
import org.cytoscape.cyndex2.internal.util.ServerManager;

public class ProfilePopupMenu extends JPopupMenu {

    public void show(Component invoker, int x, int y) {
        while (getSubElements().length > 0) {
            remove(0);
        }

        final ServerList serverList = ServerManager.INSTANCE.getAvailableServers();
        final Server selectedServer = ServerManager.INSTANCE.getSelectedServer();
        
        if (serverList.getSize() > 0) {
            for (int i = 0; i < serverList.getSize(); i++) {
                final Server server = serverList.get(i);
                
                if (!server.equals(selectedServer)) {
                
                add(new JMenuItem(new AbstractAction("<HTML>" + "<b>" + server.getUsername() + "</b>" + "<br>" + server.getUrl() + "</HTML>") 
                {
                	 public void actionPerformed(ActionEvent e) {
                		 ServerManager.INSTANCE.setSelectedServer(server);
                   }
                }));
                }
            }
            addSeparator();
        }

        
        if (selectedServer != null) {
            add(new JMenuItem(new AbstractAction("Sign Out") {
                public void actionPerformed(ActionEvent e) {
                	System.out.println("log out " + selectedServer.getUsername() + "@" + selectedServer.getUrl());
                	
                	ServerList servers = ServerManager.INSTANCE.getAvailableServers();
 
                   servers.delete(selectedServer);
                   servers.save();
                   
                   ServerManager.INSTANCE.setSelectedServer(servers.get(0));
                	
                	
                }
            }));
            addSeparator();
        }
        add(new JMenuItem(new AbstractAction("Add profile...") {
            public void actionPerformed(ActionEvent e) {
                SignInDialog signInDialog = new SignInDialog(null);
                signInDialog.setVisible(true);
            }
        }));

        super.show(invoker, x, y);
    }

}
