package org.cytoscape.hybrid.internal;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.write.CyNetworkViewWriterFactory;

public class CxTaskFactoryManager {

	// ID of the CX writer service
	private static final String CX_READER_ID = "cytoscapeCxNetworkReaderFactory";
	private static final String CX_WRITER_ID = "cxNetworkWriterFactory";
	private static final String ID_TAG = "id";


	private CyNetworkViewWriterFactory writerFactory;
	private InputStreamTaskFactory readerFactory;
	
	
	public CxTaskFactoryManager() {
	}

	public InputStreamTaskFactory getCxReaderFactory() {
		return readerFactory;
	}
	
	public CyNetworkViewWriterFactory getCxWriterFactory() {
		return writerFactory;
	}

	@SuppressWarnings("rawtypes")
	public void addWriterFactory(final CyNetworkViewWriterFactory factory, final Map properties) {
		final String id = (String) properties.get(ID_TAG);
		if (id != null && id.equals(CX_WRITER_ID)) {
			System.out.println("* CXWriterFound: " + factory);
			writerFactory = factory;
		}
	}

	@SuppressWarnings("rawtypes")
	public void removeWriterFactory(final CyNetworkViewWriterFactory factory, Map properties) {
		final String id = (String) properties.get(ID_TAG);

		if (id != null && id.equals(CX_WRITER_ID)) {
			writerFactory = null;
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void addReaderFactory(final InputStreamTaskFactory factory, final Map properties) {
		final String id = (String) properties.get(ID_TAG);
		if (id != null && id.equals(CX_READER_ID)) {
			
			System.out.println("* CXReaderFound: " + factory);
			
			readerFactory = factory;
		}
	}

	@SuppressWarnings("rawtypes")
	public void removeReaderFactory(final InputStreamTaskFactory factory, Map properties) {
		final String id = (String) properties.get(ID_TAG);

		if (id != null && id.equals(CX_READER_ID)) {
			readerFactory = null;
		}
	}
}
