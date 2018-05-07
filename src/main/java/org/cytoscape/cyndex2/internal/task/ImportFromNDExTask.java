package org.cytoscape.cyndex2.internal.task;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.cytoscape.cyndex2.internal.CyActivator;
import org.cytoscape.cyndex2.internal.CyActivator.BrowserCreationError;
import org.cytoscape.cyndex2.internal.task.NetworkImportTask.NetworkImportException;
import org.cytoscape.cyndex2.internal.ui.ImportNetworkDialog;
import org.cytoscape.cyndex2.internal.util.ExternalAppManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClient;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

import com.teamdev.jxbrowser.chromium.swing.BrowserView;

public class ImportFromNDExTask extends AbstractTask {
	
	private static Pattern uuidSearchPattern = 
			Pattern.compile("^(uuid: *)?(([0-9A-F]{8}-[0-9A-F]{4}-[1-5][0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12})|(\\\"([0-9A-F]{8}-[0-9A-F]{4}-[1-5][0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12})\\\"))$",
			Pattern.CASE_INSENSITIVE);
	

	private UUID networkUUID;
	
	public ImportFromNDExTask( final JFrame parent) {
//		this.dialog = new ImportNetworkDialog(parent,mal);

	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		// Load browserView and start external task, or show error message
//		ImportFromNDExTask task = this;
		Runnable runnable = new Runnable() {

			@Override
			public void run() {
				
				taskMonitor.setTitle("Import NDEx network");
			//	try {

				//	taskMonitor.setProgress(.5);
		//			taskMonitor.setStatusMessage("Browser created, starting CyNDEx-2");

			//		if (browserView.getParent() == null)
			//			dialog.add(browserView, BorderLayout.CENTER);

			//		ti.insertTasksAfter(task, new OpenExternalAppTask(dialog, browserView, port));
			//	} 
				
				Object[] options = { "Import", "Cancel" };
				boolean keepOpen=true;
				JPanel panel = new JPanel();
		        panel.add(new JLabel("NDEx URL or UUID"));
		        JTextField textField = new JTextField(50);
		        panel.add(textField);
		        
				NdexRestClient client = new NdexRestClient("http://public.ndexbio.org");
				NdexRestClientModelAccessLayer  mal = new NdexRestClientModelAccessLayer(client);		        
		        
				while (keepOpen) {
					int result = JOptionPane.showOptionDialog(null, panel,
							"Import NDEx Network", JOptionPane.YES_NO_OPTION,
				        JOptionPane.PLAIN_MESSAGE,
				       null, options, null);
					if (result == JOptionPane.NO_OPTION) {
						keepOpen=false;
					} else {
						System.out.println(textField.getText());
						try {
							if(foundNetwork(textField.getText(), mal)) {
								keepOpen=false;
								//import the network
								NetworkImportTask importer;
								importer = new NetworkImportTask(mal.getNdexRestClient().getUsername(), mal.getNdexRestClient().getPassword(),
									mal.getNdexRestClient().getBaseroute(),	networkUUID,
										  null);
								importer.run(taskMonitor);
							} else {
								System.out.println("give message about couldn't find network in NDEx");
							}
						} catch (IOException | NdexException e) {
							e.printStackTrace();
						} catch (NetworkImportException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}	
					}
				}	
			}

		};
		Thread thread = new Thread(runnable);

		thread.start();

	/*	new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(40000);
					taskMonitor.setStatusMessage(
							"CyNDEx2 is having trouble rendering the JXBrowser window. If the issue persists, try restarting Cytoscape.");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		while (true) {
			if (cancelled) {
				thread.interrupt();
				break;
			}
			if (complete)
				break;
		} */
	}
	
	   // check if typed text points to a valid network
    private  boolean foundNetwork(String typedText, NdexRestClientModelAccessLayer mal) throws IOException, NdexException {
    	String cleanText = typedText.replaceAll("\\s", "");
    	
    	// check if it is a pure uuid
    	Matcher m = uuidSearchPattern.matcher(cleanText);
    	if( m.matches() ) {
			String uuidStr = m.group(5);
			if ( uuidStr == null) 
				uuidStr = m.group(2);
			try {
				UUID uuid = UUID.fromString(uuidStr);
				NetworkSummary s = mal.getNetworkSummaryById(uuid);
				if (s != null) {
					networkUUID = uuid;
					return true;
				}
				return false;
			} catch( @SuppressWarnings("unused") IllegalArgumentException e ) {
				// just to be safe 
				return false;
			}
		}
    	
		//check if it is a URL to a ndex network page
    	Pattern p = Pattern.compile("^http://.*/#/network/(([0-9A-F]{8}-[0-9A-F]{4}-[1-5][0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12})|" +
		  "(\\\\\\\"([0-9A-F]{8}-[0-9A-F]{4}-[1-5][0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12})\\\\\\\"))()?$");
    	m = p.matcher(cleanText);
    	if ( m.matches()) {

    		//mal.getNetworkSummaryById(networkId, accessKey)
    	}
    	return true;
    }
}
