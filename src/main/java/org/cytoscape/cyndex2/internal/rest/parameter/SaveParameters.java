package org.cytoscape.cyndex2.internal.rest.parameter;

public class SaveParameters implements AppStatusParameters {
	public static SaveParameters INSTANCE = new SaveParameters();
	
	public Long suid;
	public String saveType;
}
