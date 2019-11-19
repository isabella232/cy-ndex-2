package org.cytoscape.cyndex2.internal.ui.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.EventObject;
import java.util.List;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.event.CellEditorListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.cytoscape.cyndex2.internal.rest.parameter.NDExImportParameters;
import org.cytoscape.cyndex2.internal.util.ErrorMessage;
import org.cytoscape.cyndex2.internal.util.Server;
import org.cytoscape.cyndex2.internal.util.ServerManager;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.model.object.network.VisibilityType;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.fasterxml.jackson.databind.ObjectMapper;

public class NetworkSummaryTableModel extends AbstractTableModel {

	private static final int IMPORT_COL = 0;
	private static final int NAME_COL = 1;
	private static final int OWNER_COL = 2;
	private static final int VISIBILITY_COL = 3;
	private static final int NODES_COL = 4;
	private static final int EDGES_COL = 5;
	private static final int CREATED_COL = 6;
	private static final int MODIFIED_COL = 7;

	private final List<NetworkSummary> networkSummaries;

	public NetworkSummaryTableModel(List<NetworkSummary> networkSummaries) {
		this.networkSummaries = List.copyOf(networkSummaries);
	}

	public static class TimestampRenderer extends DefaultTableCellRenderer {
		DateFormat formatter;

		public TimestampRenderer() {
			super();
		}

		public void setValue(Object value) {
			if (formatter == null) {
				formatter = DateFormat.getDateInstance();
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

	public static class ImportButtonRenderer implements TableCellRenderer {
		JButton button = new JButton("Import");

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			button.setBorder(BorderFactory.createEmptyBorder());
			button.setToolTipText(value.toString());
			return button;
		}
	}
	
	private static void load(final NetworkSummary networkSummary) {

		//final Component me = this;
		
		SwingWorker<Integer, Integer> worker = new SwingWorker<Integer, Integer>() {

			@Override
			protected Integer doInBackground() throws Exception {

				// For entire network, we will query again, hence will check credential
				final Server selectedServer = ServerManager.INSTANCE.getServer();
				final NdexRestClientModelAccessLayer mal = selectedServer.getModelAccessLayer();
				boolean success = selectedServer.check(mal);
				if (success) {
					// The network to copy from.
//                        NetworkSummary networkSummary = NetworkManager.INSTANCE.getSelectedNetworkSummary();
					UUID uuid = networkSummary.getExternalId();
					try {
						// ProvenanceEntity provenance = mal.getNetworkProvenance(id.toString());

						String REST_URI = "http://localhost:1234/cyndex2/v1/networks";
						HttpClient httpClient = HttpClients.createDefault();
						final URI uri = URI.create(REST_URI);
						final HttpPost post = new HttpPost(uri.toString());
						post.setHeader("Content-type", "application/json");
						
						NDExImportParameters importParameters = new NDExImportParameters(uuid.toString(), selectedServer.getUsername(), selectedServer.getPassword(),
								selectedServer.getUrl(), null, null);
						ObjectMapper objectMapper = new ObjectMapper();

						post.setEntity(new StringEntity(objectMapper.writeValueAsString(importParameters)));

						httpClient.execute(post);

					}

					/*
					 * catch (IOException ex) { JOptionPane.showMessageDialog(me,
					 * ErrorMessage.failedToParseJson, "Error", JOptionPane.ERROR_MESSAGE); return
					 * -1; }
					 */
					catch (RuntimeException ex2) {
						JOptionPane.showMessageDialog(null, "This network can't be imported to cytoscape. Cause: " + ex2.getMessage(),
								"Error", JOptionPane.ERROR_MESSAGE);
						ex2.printStackTrace();
						return -1;
					}
				} else {
					JOptionPane.showMessageDialog(null, ErrorMessage.failedServerCommunication, "Error", JOptionPane.ERROR_MESSAGE);
				}
				return 1;
			}
		};
		worker.execute();
//        findNetworksDialog.setFocusOnDone();
//        this.setVisible(false);
	}
	
	public static class ImportButtonEditor extends DefaultCellEditor {

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
		return 8;
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
		case CREATED_COL:
			return Timestamp.class;
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
		case CREATED_COL:
			return networkSummary.getCreationTime();
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
		case CREATED_COL:
			return "created";
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
