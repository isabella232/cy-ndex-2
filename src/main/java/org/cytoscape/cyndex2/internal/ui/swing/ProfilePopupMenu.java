package org.cytoscape.cyndex2.internal.ui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.ServerKey;
import org.cytoscape.cyndex2.internal.util.ServerList;
import org.cytoscape.cyndex2.internal.util.ServerManager;

public class ProfilePopupMenu extends JPopupMenu {

	public void show(Component invoker, int x, int y) {
		while (getSubElements().length > 0) {
			remove(0);
		}

		final ServerList serverList = ServerManager.INSTANCE.getAvailableServers();
		final Server selectedServer = ServerManager.INSTANCE.getSelectedServer();

		if (selectedServer == null) {

		} else {
			add(new JMenuItem(new AbstractAction("Remove Profile") {
				public void actionPerformed(ActionEvent e) {
					System.out.println("log out " + selectedServer.getUsername() + "@" + selectedServer.getUrl());
					ServerManager.INSTANCE.removeServer(selectedServer);
				}
			}));
			addSeparator();
		}

		List<Server> serverItems = serverList.stream().filter(server -> !server.equals(selectedServer))
				.collect(Collectors.toList());

		if (serverItems.size() > 0) {
			serverItems.stream().forEach(server -> {
				JMenuItem jMenuItem = new JMenuItem(new AbstractAction(
						"<HTML>" + "<b>" + server.getUsername() + "</b>" + "<br>" + server.getUrl() + "</HTML>") {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						ServerManager.INSTANCE.setSelectedServer(new ServerKey(server));
					}
				});
				add(jMenuItem);
			});

			addSeparator();
		}

		add(new JMenuItem(new AbstractAction("Add new profile...") {
			public void actionPerformed(ActionEvent e) {
				SignInDialog signInDialog = new SignInDialog(null);
				signInDialog.setVisible(true);
			}
		}));

		super.show(invoker, x, y);
	}

}
