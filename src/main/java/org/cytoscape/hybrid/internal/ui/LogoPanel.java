package org.cytoscape.hybrid.internal.ui;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import static org.cytoscape.hybrid.internal.ui.UiTheme.*;

public class LogoPanel extends JPanel {

	private static final long serialVersionUID = -1708511648954252406L;
	
	private static final Icon NDEX_LOGO = new ImageIcon(
			LogoPanel.class.getClassLoader().getResource("images/ndex.png"));

	public LogoPanel() {
		// Title w/icon
		final JLabel logo = new JLabel();
		logo.setOpaque(false);
		logo.setIcon(NDEX_LOGO);
		logo.setHorizontalAlignment(SwingConstants.CENTER);
		logo.setHorizontalTextPosition(SwingConstants.CENTER);

		final Border paddingBorder = BorderFactory.createEmptyBorder(5, 0, 5, 0);
		logo.setBorder(paddingBorder);
		setBackground(COLOR_ENABLED);
		setLayout(new BorderLayout());
		add(logo, BorderLayout.CENTER);
	}

}
