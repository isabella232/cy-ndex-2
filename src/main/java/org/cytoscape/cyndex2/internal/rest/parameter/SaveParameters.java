package org.cytoscape.cyndex2.internal.rest.parameter;

public class SaveParameters implements AppStatusParameters {
	public static Long suid;
	
	public static SaveParameters INSTANCE = new SaveParameters();
	
	public String saveType;
}
