package org.cytoscape.cyndex2.external;

import org.cytoscape.cyndex2.internal.rest.parameter.AppStatusParameters;

public class SaveParameters implements AppStatusParameters {
	public static SaveParameters INSTANCE = new SaveParameters();
	
	public Long suid;
	public String saveType;
}
