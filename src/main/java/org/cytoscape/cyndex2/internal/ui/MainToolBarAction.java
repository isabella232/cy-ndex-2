package org.cytoscape.cyndex2.internal.ui;

import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.cyndex2.internal.util.IconUtil;
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
	
	private void showPopupMenu(JComponent comp) {
		JPopupMenu popup = new JPopupMenu();
		
		DialogTaskManager taskManager = serviceRegistrar.getService(DialogTaskManager.class);
		Font font = IconUtil.getAppFont(23f);
		int iconSize = 24;
		
		{
			Icon icon = new TextIcon(IconUtil.LAYERED_OPEN_ICON, font, IconUtil.LAYERED_OPEN_SAVE_COLORS, iconSize, iconSize, 1);
			JMenuItem mi = new JMenuItem(StringResources.NDEX_OPEN, icon);
			mi.addActionListener(evt -> taskManager.execute(importTaskFactory.createTaskIterator()));
			mi.setEnabled(importTaskFactory.isReady());
			popup.add(mi);
		}
		{
			Icon icon = new TextIcon(IconUtil.LAYERED_SAVE_ICON, font, IconUtil.LAYERED_OPEN_SAVE_COLORS, iconSize, iconSize, 1);
			JMenuItem mi = new JMenuItem(StringResources.NDEX_SAVE, icon);
			mi.addActionListener(evt -> taskManager.execute(saveTaskFactory.createTaskIterator()));
			mi.setEnabled(saveTaskFactory.isReady());
			popup.add(mi);
		}
		LookAndFeelUtil.makeSmall(popup);
		
		popup.show(comp, 0, comp.getSize().height);
	}
}
