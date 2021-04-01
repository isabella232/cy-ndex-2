package org.cytoscape.cyndex2.internal.ui;

import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.ui.swing.SignInDialog;
import org.cytoscape.cyndex2.internal.util.IconUtil;
import org.cytoscape.cyndex2.internal.util.ServerManager;
import org.cytoscape.cyndex2.internal.util.StringResources;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.util.swing.TextIcon;
import org.cytoscape.work.swing.DialogTaskManager;


@SuppressWarnings("serial")
public class MainToolBarAction extends AbstractCyAction {

	private static final String TITLE = "Open or Save Networks in NDEx...";
	private static final String DESCRIPTION = "Open or Save Networks and Collections in NDEx, the Cloud Storage for the Cytoscape Cyberinfrastructure";
	private final ImportNetworkFromNDExTaskFactory importTaskFactory;
	private final SaveNetworkToNDExTaskFactory saveTaskFactory;
	private final CyServiceRegistrar serviceRegistrar;
	
	public MainToolBarAction(
			ImportNetworkFromNDExTaskFactory importTaskFactory,
			SaveNetworkToNDExTaskFactory saveTaskFactory,
			CyServiceRegistrar serviceRegistrar
	) {
		super(TITLE);
		this.importTaskFactory = importTaskFactory;
		this.saveTaskFactory = saveTaskFactory;
		this.serviceRegistrar = serviceRegistrar;
		
		inToolBar = true;
		insertToolbarSeparatorAfter = true;
		toolbarGravity = Short.MIN_VALUE;
		
		putValue(SHORT_DESCRIPTION, TITLE); // Tooltip's short description
		putValue(LONG_DESCRIPTION, DESCRIPTION);
		putValue(LARGE_ICON_KEY, IconUtil.getNdexIcon());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JComponent comp = e.getSource() instanceof JComponent ? (JComponent) e.getSource()
				: serviceRegistrar.getService(CySwingApplication.class).getJToolBar();
		showPopupMenu(comp);
	}
	
	@Override
	public boolean isEnabled() {
		return importTaskFactory.isReady() || saveTaskFactory.isReady();
	}
	
	private JMenuItem getSignInMenuItem(final Font font, final int iconSize) {
		final Icon icon = new TextIcon(IconUtil.ICON_NDEX_ACCOUNT, font, iconSize, iconSize);
		final JMenuItem mi = new JMenuItem(new AbstractAction("Get on NDEx", icon) {
			public void actionPerformed(ActionEvent e) {
				JFrame parent = serviceRegistrar.getService(CySwingApplication.class).getJFrame();
				
				SignInDialog signInDialog = new SignInDialog(null);
				signInDialog.setLocationRelativeTo(parent);
				signInDialog.setVisible(true);
			}
		});
		return mi;
	}
	
	private JMenuItem getMyNetworksMenuItem(final Font font, final int iconSize) {
		final Icon icon = new TextIcon(IconUtil.ICON_NDEX_ACCOUNT, font, iconSize, iconSize);
		final JMenuItem mi = new JMenuItem(new AbstractAction("My Networks", icon) {
			public void actionPerformed(ActionEvent e) {
				
			}
		});
		return mi;
	}
	
	private void showPopupMenu(JComponent comp) {
		JPopupMenu popupMenu = new JPopupMenu();
		
		DialogTaskManager taskManager = serviceRegistrar.getService(DialogTaskManager.class);
		Font font = IconUtil.getAppFont(23f);
		int iconSize = 24;
		
		{
			JMenuItem mi = ServerManager.INSTANCE.getAvailableServers().getSize() == 0 ? getSignInMenuItem(font, iconSize) : getMyNetworksMenuItem(font, iconSize);
			popupMenu.add(mi);
		}
		
		popupMenu.addSeparator();
		
		{
			Icon icon = new TextIcon(IconUtil.LAYERED_OPEN_ICON, font, IconUtil.LAYERED_OPEN_SAVE_COLORS, iconSize, iconSize, 1);
			JMenuItem mi = new JMenuItem(StringResources.NDEX_OPEN, icon);
			mi.addActionListener(evt -> taskManager.execute(importTaskFactory.createTaskIterator()));
			mi.setEnabled(importTaskFactory.isReady());
			popupMenu.add(mi);
		}
		{
			Icon icon = new TextIcon(IconUtil.LAYERED_SAVE_ICON, font, IconUtil.LAYERED_OPEN_SAVE_COLORS, iconSize, iconSize, 1);
			JMenuItem mi = new JMenuItem(StringResources.NDEX_SAVE, icon);
			mi.addActionListener(evt -> taskManager.execute(saveTaskFactory.createTaskIterator()));
			mi.setEnabled(saveTaskFactory.isReady());
			popupMenu.add(mi);
		}
		
		popupMenu.addSeparator();
		
		{
			JMenuItem mi = new JMenuItem("CyNDEx-2 Version " + CyActivator.getAppVersion());
			mi.setEnabled(false);
			popupMenu.add(mi);
		}
		
		LookAndFeelUtil.makeSmall(popupMenu);
		
		popupMenu.show(comp, 0, comp.getSize().height);
	}
}
