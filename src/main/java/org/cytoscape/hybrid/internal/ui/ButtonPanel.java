package org.cytoscape.hybrid.internal.ui;

import static org.cytoscape.hybrid.internal.ui.UiTheme.COLOR_DISABLED;
import static org.cytoscape.hybrid.internal.ui.UiTheme.COLOR_DISABLED_FONT;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.SwingPropertyChangeSupport;

public class ButtonPanel extends JPanel {

	private static final long serialVersionUID = -3520245233390131451L;

	// Theme for this panel components
	private static final Font BUTTON_FONT1 = new Font("helvatica", Font.PLAIN, 18);
	private static final Font BUTTON_FONT2 = new Font("helvatica", Font.BOLD, 18);
	private static final Color SEARCH_COLOR = Color.decode("#FF4081");
	private static final Color CLEAR_COLOR = Color.decode("#FAFFEA");
	private static final Color SEARCH_COLOR_FONT = Color.WHITE;
	private static final Color CLEAR_COLOR_FONT = Color.decode("#777777");

	private JPanel clearPanel;
	private JPanel searchPanel;
	private JLabel searchButton;
	private JLabel clearButton;

	private final PropertyChangeSupport pcs;

	public ButtonPanel() {
		this.pcs = new SwingPropertyChangeSupport(this);
		init();
	}

	private final void init() {
		setLayout(new GridLayout(1, 2));

		clearPanel = new JPanel();
		clearPanel.setLayout(new GridLayout(1, 1));
		clearPanel.setBackground(CLEAR_COLOR);

		searchPanel = new JPanel();
		searchPanel.setLayout(new GridLayout(1, 1));
		searchPanel.setBackground(SEARCH_COLOR);

		final JLabel clearButton = getClearButton();
		clearButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				System.out.println("Button Click clear $$$$$$$$$$$$$$$$$: " + clearButton.isEnabled());
				if (clearButton.isEnabled()) {
					PropertyChangeEvent event = new PropertyChangeEvent(this, SearchPane.ACTION_CLEAR, null, null);
					pcs.firePropertyChange(event);
				}
			}
		});
		clearPanel.add(clearButton);

		this.add(clearPanel);

		final JLabel searchButton = getSearchButton();
		searchButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {

				System.out.println("Button Click search $$$$$$$$$$$$$$$$$: " + searchButton.isEnabled());
				if (searchButton.isEnabled()) {
					pcs.firePropertyChange(SearchPane.ACTION_SEARCH, null, null);
				}
			}
		});

		searchPanel.add(searchButton);
		this.add(searchPanel);
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener l) {
		pcs.addPropertyChangeListener(l);
	}

	private final JLabel getClearButton() {
		clearButton = new JLabel("CLEAR");
		clearButton.addMouseListener(new ButtonMouseAdapter());
		clearButton.setOpaque(false);
		clearButton.setForeground(CLEAR_COLOR_FONT);
		clearButton.setHorizontalAlignment(SwingConstants.CENTER);
		clearButton.setHorizontalTextPosition(SwingConstants.CENTER);
		clearButton.setFont(BUTTON_FONT1);
		final Border paddingBorder2 = BorderFactory.createEmptyBorder(10, 0, 10, 0);
		clearButton.setBorder(paddingBorder2);

		return clearButton;
	}

	private final JLabel getSearchButton() {
		searchButton = new JLabel("SEARCH");
		searchButton.addMouseListener(new ButtonMouseAdapter());
		searchButton.setToolTipText("Click to show search result in a new window...");
		searchButton.setOpaque(false);
		searchButton.setForeground(SEARCH_COLOR_FONT);
		searchButton.setHorizontalAlignment(SwingConstants.CENTER);
		searchButton.setHorizontalTextPosition(SwingConstants.CENTER);
		searchButton.setFont(BUTTON_FONT1);
		final Border paddingBorder2 = BorderFactory.createEmptyBorder(10, 0, 10, 0);
		searchButton.setBorder(paddingBorder2);

		return searchButton;
	}

	protected final void setButtonsEnabled(final Boolean flag) {

		this.clearPanel.setEnabled(flag);
		this.searchPanel.setEnabled(flag);
		this.clearButton.setEnabled(flag);
		this.searchButton.setEnabled(flag);

		if (flag) {
			this.clearPanel.setBackground(CLEAR_COLOR);
			this.searchPanel.setBackground(SEARCH_COLOR);

			this.clearButton.setForeground(CLEAR_COLOR_FONT);
			this.searchButton.setForeground(SEARCH_COLOR_FONT);
		} else {
			this.clearPanel.setBackground(COLOR_DISABLED);
			this.searchPanel.setBackground(COLOR_DISABLED);

			this.clearButton.setForeground(COLOR_DISABLED_FONT);
			this.searchButton.setForeground(COLOR_DISABLED_FONT);
		}
	}

	private final class ButtonMouseAdapter extends MouseAdapter {

		@Override
		public void mouseExited(MouseEvent e) {

			final Component button = e.getComponent();
			if (button.isEnabled()) {
				button.setFont(BUTTON_FONT1);
			}
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			Component button = e.getComponent();
			if (button.isEnabled()) {
				button.setFont(BUTTON_FONT2);
			}
		}
	}

}
