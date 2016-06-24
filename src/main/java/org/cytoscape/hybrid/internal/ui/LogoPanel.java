package org.cytoscape.hybrid.internal.ui;

import static org.cytoscape.hybrid.internal.ui.UiTheme.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.cytoscape.hybrid.internal.login.Credential;
import org.cytoscape.hybrid.internal.login.LoginManager;

public class LogoPanel extends JPanel implements PropertyChangeListener{

	private static final long serialVersionUID = -1708511648954252406L;
	
	private static final Icon NDEX_LOGO = new ImageIcon(
			LogoPanel.class.getClassLoader().getResource("images/ndex.png"));

	final JLabel logo;
	
	
	public LogoPanel(final LoginManager manager) {
		// Title w/icon
		logo = new JLabel("(Not Logged In)");
		logo.setOpaque(false);
		logo.setIcon(NDEX_LOGO);
		logo.setHorizontalAlignment(SwingConstants.LEFT);
		logo.setIconTextGap(10);
		logo.setFont(DEF_FONT);

		final Border paddingBorder = BorderFactory.createEmptyBorder(5, 15, 5, 0);
		logo.setBorder(paddingBorder);
		setBackground(COLOR_ENABLED);
		setLayout(new BorderLayout());
		add(logo, BorderLayout.CENTER);
		manager.addPropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		System.out.println("Got PCE " + evt);
		if(evt.getPropertyName().equals(LoginManager.EVENT_LOGIN)) {
			Credential credential = (Credential) evt.getNewValue();
			
			if(credential != null) {
				this.logo.setText("ID: " + credential.getUserName() + " (" + credential.getServerName() + ")");
			}
		}
		
	}

}
