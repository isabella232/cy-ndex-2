package org.cytoscape.cyndex2.internal.singletons;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import org.cxio.core.interfaces.AspectElement;
import org.cxio.metadata.MetaDataCollection;
import org.cytoscape.cyndex2.internal.util.StringResources;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.ndexbio.model.cx.NamespacesElement;
import org.ndexbio.model.cx.Provenance;

public class CXInfoHolder {

	private Map<String, Collection<AspectElement>> opaqueAspectsTable;

	private final CyNetwork network;

	public CXInfoHolder(CyNetwork network) {
		this.network = network;
		this.opaqueAspectsTable = new TreeMap<>();

	}

	private static void createColumnIfNotExists(CyTable table, String name, Class<?> clz) {
		if (table.getColumn(name) == null) {
			table.createColumn(name, clz, true);
		}
	}

	public void addNodeMapping(Long cyNodeId, Long cxNodeId) {
		CyTable table = network.getTable(CyNode.class, CyNetwork.HIDDEN_ATTRS);
		createColumnIfNotExists(table, StringResources.CX_ID_COLUMN, Long.class);
		CyRow row = table.getRow(cyNodeId);
		if (row == null) {
			System.out.println("Row with id " + cyNodeId + " not found");
		}
		row.set(StringResources.CX_ID_COLUMN, cxNodeId);
	}

	public void addEdgeMapping(Long cyEdgeId, Long cxEdgeId) {
		CyTable table = network.getTable(CyEdge.class, CyNetwork.HIDDEN_ATTRS);
		createColumnIfNotExists(table, StringResources.CX_ID_COLUMN, Long.class);
		CyRow row = table.getRow(cyEdgeId);
		if (row == null) {
			System.out.println("Row with id " + cyEdgeId + " not found");
		}
		row.set(StringResources.CX_ID_COLUMN, cxEdgeId);
	}

	public Long getCXNodeId(Long cyNodeId) {
		CyTable table = network.getTable(CyNode.class, CyNetwork.HIDDEN_ATTRS);
		createColumnIfNotExists(table, StringResources.CX_ID_COLUMN, Long.class);
		CyRow row = table.getRow(cyNodeId);
		return row.get(StringResources.CX_ID_COLUMN, Long.class);
	}

	public Long getCXEdgeId(Long cyEdgeId) {
		CyTable table = network.getTable(CyEdge.class, CyNetwork.HIDDEN_ATTRS);
		createColumnIfNotExists(table, StringResources.CX_ID_COLUMN, Long.class);
		CyRow row = table.getRow(cyEdgeId);
		return row.get(StringResources.CX_ID_COLUMN, Long.class);
	}

	public MetaDataCollection getMetadata() {
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		createColumnIfNotExists(table, StringResources.METADATA_COLUMN, String.class);
		CyRow row = table.getRow(network.getSUID());
		String metadataStr = row.get(StringResources.METADATA_COLUMN, String.class);
		try {
			return MetaDataCollection.createInstanceFromJson(metadataStr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public static void setMetadata(CyNetwork network, MetaDataCollection metadata) {
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		createColumnIfNotExists(table, StringResources.METADATA_COLUMN, String.class);
		CyRow row = table.getRow(network.getSUID());
		row.set(StringResources.METADATA_COLUMN, metadata.toString());
	}

	@SuppressWarnings("unused")
	private static String serializeObject(Object map) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(map);
			oos.close();
			baos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return baos.toString();
	}
	
	@SuppressWarnings("unchecked")
	private static Map<String, ?> deserializeObject(String map) throws ClassNotFoundException, IOException{
		BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(map.getBytes()));
		ObjectInputStream ois = new ObjectInputStream(in);
		return (Map<String, Object>) ois.readObject();
	}

	

	@SuppressWarnings("unchecked")
	public static void setOpaqueAspectsTable(CyNetwork network, Map<String, Collection<AspectElement>> opaqueAspectsTable) {
		
		String ser = serializeObject(opaqueAspectsTable);
		System.out.println("OPAQUE ASPECTS: " + ser);
		
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		createColumnIfNotExists(table, StringResources.OPAQUE_ASPECTS_COLUMN, String.class);
		CyRow row = table.getRow(network.getSUID());
		row.set(StringResources.OPAQUE_ASPECTS_COLUMN, ser);
		
		try {
			opaqueAspectsTable = (Map<String, Collection<AspectElement>>) deserializeObject(ser);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void setProvenance(CyNetwork network, Provenance provenance) {
		String ser = serializeObject(provenance);
		System.out.println("Pro: " + ser);
		
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		createColumnIfNotExists(table, StringResources.PROVENANCE_COLUMN, String.class);
		CyRow row = table.getRow(network.getSUID());
		row.set(StringResources.PROVENANCE_COLUMN, ser);
		
		try {
			Provenance pro = (Provenance) deserializeObject(ser);
			System.out.println("DESER: " + pro);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void setNamespaces(CyNetwork network, NamespacesElement namespaces) {
		String ser = serializeObject(namespaces);
		System.out.println("NE: " + ser);
		
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		createColumnIfNotExists(table, StringResources.NAMESPACES_COLUMN, String.class);
		CyRow row = table.getRow(network.getSUID());
		row.set(StringResources.NAMESPACES_COLUMN, ser);
		
		try {
			NamespacesElement ne = (NamespacesElement) deserializeObject(ser);
			System.out.println("DESER: " + ne);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public NamespacesElement getNamespaces() {
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		createColumnIfNotExists(table, StringResources.NAMESPACES_COLUMN, String.class);
		CyRow row = table.getRow(network.getSUID());
		String namespacesStr = row.get(StringResources.NAMESPACES_COLUMN, String.class);
		try {
			NamespacesElement namespaces = (NamespacesElement) deserializeObject(namespacesStr);
			return namespaces;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Provenance getProvenance() {
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		createColumnIfNotExists(table, StringResources.PROVENANCE_COLUMN, String.class);
		CyRow row = table.getRow(network.getSUID());
		String provenanceStr = row.get(StringResources.PROVENANCE_COLUMN, String.class);
		try {
			Provenance provenance = (Provenance) deserializeObject(provenanceStr);
			return provenance;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Collection<AspectElement>> getOpaqueAspectsTable() {
		CyTable table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
		createColumnIfNotExists(table, StringResources.OPAQUE_ASPECTS_COLUMN, String.class);
		CyRow row = table.getRow(network.getSUID());
		String opaqueTable = row.get(StringResources.OPAQUE_ASPECTS_COLUMN, String.class);
		
		try {
			opaqueAspectsTable = (Map<String, Collection<AspectElement>>) deserializeObject(opaqueTable);
			return opaqueAspectsTable;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
