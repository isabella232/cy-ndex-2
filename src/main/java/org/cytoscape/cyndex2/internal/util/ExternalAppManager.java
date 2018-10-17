package org.cytoscape.cyndex2.internal.util;

import org.cytoscape.cyndex2.internal.task.OpenBrowseTaskFactory;

public class ExternalAppManager {

	public static final String APP_NAME_SAVE = "save";
	public static final String APP_NAME_LOAD = "choose";
	
	public static final String SAVE_NETWORK = "network";
	public static final String SAVE_COLLECTION = "collection";
	
	public static String appName;
	public static String saveType;
	
	private static boolean loadFailed = false;
	
	public static void setLoadFailed(String reason){
		OpenBrowseTaskFactory.getEntry().setDisabled(reason);
		loadFailed = true;
	}
	
	public static boolean loadFailed(){
		return loadFailed;
	}
}
