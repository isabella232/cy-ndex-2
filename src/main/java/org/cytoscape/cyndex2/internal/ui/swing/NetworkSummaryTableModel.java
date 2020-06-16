package org.cytoscape.cyndex2.internal.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.util.swing.IconManager;
import org.cytoscape.util.swing.TextIcon;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.VisibilityType;

public class NetworkSummaryTableModel extends AbstractTableModel {

	public static final int IMPORT_COL = 0;
	public static final int NAME_COL = 1;
	public static final int OWNER_COL = 2;
	public static final int VISIBILITY_COL = 3;
	public static final int NODES_COL = 4;
	public static final int EDGES_COL = 5;
	public static final int MODIFIED_COL = 6;

	private final List<NetworkSummary> networkSummaries;

	private Consumer<NetworkSummary> networkSummaryConsumer;
	
	public NetworkSummaryTableModel(List<NetworkSummary> networkSummaries, Consumer<NetworkSummary> networkSummaryConsumer) {
		this.networkSummaries = new ArrayList<NetworkSummary>(networkSummaries);
		this.networkSummaryConsumer = networkSummaryConsumer;
	}

	public static class TimestampRenderer extends DefaultTableCellRenderer {
		DateFormat formatter;

		public TimestampRenderer() {
			super();
		}

		public void setValue(Object value) {
			if (formatter == null) {
				formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
			}
			setText((value == null && value instanceof Timestamp) ? "" : formatter.format(((Timestamp) value)));
		}
	}

	public static class VisibilityTypeRenderer extends DefaultTableCellRenderer {
		public VisibilityTypeRenderer() {
			super();
		}

		public void setValue(Object value) {
			setText((value == null && value instanceof VisibilityType) ? "" : ((VisibilityType) value).name());
		}
	}

	private static TextIcon getImportIcon() {
		final IconManager iconManager = CyServiceModule.INSTANCE.getService(IconManager.class);
		final TextIcon importIcon = new TextIcon(iconManager.ICON_ARROW_CIRCLE_O_DOWN, iconManager.getIconFont(24), new Color(28,140,46), 24, 24);
		return importIcon;
	}
	
	public static class ImportButtonRenderer implements TableCellRenderer {
		JButton button = new JButton(getImportIcon());

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			button.setBorder(BorderFactory.createEmptyBorder());
			button.setBorderPainted(false);
			button.setContentAreaFilled(false);
			button.setToolTipText("Import network to Cytoscape");
			return button;
		}
	}
	
	private void load(final NetworkSummary networkSummary) {
		networkSummaryConsumer.accept(networkSummary);
	}
	
	public class ImportButtonEditor extends DefaultCellEditor {

    protected JButton button;
    private NetworkSummary networkSummary;
    private boolean isPushed;

    public ImportButtonEditor(JCheckBox checkBox) {
        super(checkBox);
        button = new JButton("Import");
        button.setOpaque(true);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              load(networkSummary);
              fireEditingStopped();
            }
        });
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        if (isSelected) {
            button.setForeground(table.getSelectionForeground());
            button.setBackground(table.getSelectionBackground());
        } else {
            button.setForeground(table.getForeground());
            button.setBackground(table.getBackground());
        }
        networkSummary = (NetworkSummary) value;
        button.setText("Importing");
      
        isPushed = true;
        return button;
    }

    @Override
    public Object getCellEditorValue() {
      
        isPushed = false;
        return networkSummary;
    }

    @Override
    public boolean stopCellEditing() {
        isPushed = false;
        
        return super.stopCellEditing();
    }
}


	@Override
	public int getColumnCount() {
		return 7;
	}

	@Override
	public int getRowCount() {
		return networkSummaries.size();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch (columnIndex) {
		case IMPORT_COL:
			return NetworkSummary.class;
		case NAME_COL:
			return String.class;
		case OWNER_COL:
			return String.class;
		case VISIBILITY_COL:
			return VisibilityType.class;
		case NODES_COL:
			return Integer.class;
		case EDGES_COL:
			return Integer.class;
		case MODIFIED_COL:
			return Timestamp.class;
		default:
			throw new IllegalArgumentException("Column at index " + columnIndex + " does not exist.");
		}
	}

	@Override
	public Object getValueAt(int arg0, int arg1) {
		final NetworkSummary networkSummary = networkSummaries.get(arg0);
		switch (arg1) {
		case IMPORT_COL:
			return networkSummary;
		case NAME_COL:
			return networkSummary.getName();
		case OWNER_COL:
			return networkSummary.getOwner();
		case VISIBILITY_COL:
			return networkSummary.getVisibility();
		case NODES_COL:
			return networkSummary.getNodeCount();
		case EDGES_COL:
			return networkSummary.getEdgeCount();
		case MODIFIED_COL:
			return networkSummary.getModificationTime();
		default:
			throw new IllegalArgumentException("Column at index " + arg1 + " does not exist.");
		}
	}

	@Override
	public String getColumnName(int columnIndex) {
		switch (columnIndex) {
		case IMPORT_COL:
			return "";
		case NAME_COL:
			return "name";
		case OWNER_COL:
			return "owner";
		case VISIBILITY_COL:
			return "visibility";
		case NODES_COL:
			return "nodes";
		case EDGES_COL:
			return "edges";
		case MODIFIED_COL:
			return "modified";
		default:
			throw new IllegalArgumentException("Column at index " + columnIndex + " does not exist.");
		}
	}

	public boolean isCellEditable(int row, int column) {
		return column == IMPORT_COL;
	}
}
