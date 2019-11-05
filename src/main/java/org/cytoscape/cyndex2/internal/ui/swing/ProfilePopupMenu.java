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

        if (serverList.getSize() > 0) {
            for (int i = 0; i < serverList.getSize(); i++) {
                final Server server = serverList.get(i);
                add(new JMenuItem("<HTML>" + "<b>" + server.getUsername() + "</b>" + "<br>" + server.getUrl() + "</HTML>"));
            }
            addSeparator();
        }

        final Server selectedServer = ServerManager.INSTANCE.getSelectedServer();
        if (selectedServer != null) {
            add(new JMenuItem(new AbstractAction("Sign Out " + selectedServer.getName()) {
                public void actionPerformed(ActionEvent e) {

                }
            }));
            addSeparator();
        }
        add(new JMenuItem(new AbstractAction("Add profile...") {
            public void actionPerformed(ActionEvent e) {
                SignInDialog signInDialog = new SignInDialog(null);
                //signInDialog.setLocationRelativeTo(null);
                signInDialog.setVisible(true);
                java.awt.EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        signInDialog.toFront();
                        signInDialog.repaint();
                    }
                });
            }
        }));

        super.show(invoker, x, y);
    }

}
