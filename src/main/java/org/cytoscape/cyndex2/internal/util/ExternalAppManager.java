package org.cytoscape.cyndex2.internal.util;

import java.awt.Dialog.ModalityType;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.cytoscape.cyndex2.internal.task.OpenBrowseTaskFactory;

public class ExternalAppManager {

	public static final String APP_NAME_SAVE = "save";
	public static final String APP_NAME_LOAD = "choose";
	
	public static final String SAVE_NETWORK = "network";
	public static final String SAVE_COLLECTION = "collection";
	

	public static String query;
	public static String appName;
	public static String saveType;
	private static JDialog dialog;
	
	public static boolean busy = false;
	private static boolean loadFailed = false;
	
	public static JDialog getDialog(JFrame parent){
		if (dialog == null){
			dialog = new JDialog(parent, "CyNDEx2 Browser", ModalityType.APPLICATION_MODAL);
			// ensure modality type
			dialog.getModalityType();
		}
		return dialog;
	}
	
	public static void setLoadFailed(String reason){
		OpenBrowseTaskFactory.getEntry().setDisabled(reason);
		loadFailed = true;
	}
	
	public static boolean loadFailed(){
		return loadFailed;
	}
}
