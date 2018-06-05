package org.cytoscape.cyndex2.errors;

public class BrowserCreationError extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4687535679654703495L;

	public BrowserCreationError(String message) {
		super("JXBrowser instance creation failed.\n" + message);
	}
}
