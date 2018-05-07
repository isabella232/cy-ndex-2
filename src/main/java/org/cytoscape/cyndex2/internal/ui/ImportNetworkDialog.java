package org.cytoscape.cyndex2.internal.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.ndexbio.model.exceptions.NdexException;
import org.ndexbio.model.object.network.NetworkSummary;
import org.ndexbio.rest.client.NdexRestClientModelAccessLayer;

public class ImportNetworkDialog extends JDialog implements ActionListener, PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String typedText = null;
    private JTextField textField;
 //   private DialogDemo dd;
 
//    private String magicWord;
    private JOptionPane optionPane;
 
    private String btnString1 = "Import";
    private String btnString2 = "Cancel";
    private NdexRestClientModelAccessLayer mal;

	private static Pattern uuidSearchPattern = 
			Pattern.compile("^(uuid: *)?(([0-9A-F]{8}-[0-9A-F]{4}-[1-5][0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12})|(\\\"([0-9A-F]{8}-[0-9A-F]{4}-[1-5][0-9A-F]{3}-[89AB][0-9A-F]{3}-[0-9A-F]{12})\\\"))$",
			Pattern.CASE_INSENSITIVE);
    
    /**
     * Returns null if the typed string was invalid;
     * otherwise, returns the string as the user entered it.
     */
    public String getValidatedText() {
        return typedText;
    }
 
    /** Creates the reusable dialog. */
    public ImportNetworkDialog(JFrame aFrame, NdexRestClientModelAccessLayer  mal) {
        super(aFrame, true);
 
		setSize(500, 200);
		setLocationRelativeTo(aFrame);
		
        textField = new JTextField(50);
        this.mal = mal;
 
        //Create an array of the text and components to be displayed.
        String msgString1 = "URL or UUID of the netork";
     //   String msgString2 = "(The answer is \"" + magicWord
     //                         + "\".)";
        Object[] array = {msgString1, /*msgString2,*/ textField};
 
        //Create an array specifying the number of dialog buttons
        //and their text.
        Object[] options = {btnString1, btnString2};
 
        //Create the JOptionPane.
        optionPane = new JOptionPane(array,
                                    JOptionPane.QUESTION_MESSAGE,
                                    JOptionPane.YES_NO_OPTION,
                                    null,
                                    options,
                                    options[0]);
 
        //Make this dialog display it.
        setContentPane(optionPane);
 
        //Handle window closing correctly.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
                @Override
				public void windowClosing(WindowEvent we) {
                /*
                 * Instead of directly closing the window,
                 * we're going to change the JOptionPane's
                 * value property.
                 */
                    optionPane.setValue(new Integer(
                                        JOptionPane.CLOSED_OPTION));
            }
        });
 
        //Ensure the text field always gets the first focus.
        addComponentListener(new ComponentAdapter() {
            @Override
			public void componentShown(ComponentEvent ce) {
                textField.requestFocusInWindow();
            }
        });
 
        //Register an event handler that puts the text into the option pane.
        textField.addActionListener(this);
 
        //Register an event handler that reacts to option pane state changes.
        optionPane.addPropertyChangeListener(this);
    }
 
    /** This method handles events for the text field. */
    public void actionPerformed(ActionEvent e) {
        optionPane.setValue(btnString1);
    }
 
    /** This method reacts to state changes in the option pane. */
    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();
 
        if (isVisible()
         && (e.getSource() == optionPane)
         && (JOptionPane.VALUE_PROPERTY.equals(prop) ||
             JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
            Object value = optionPane.getValue();
 
            if (value == JOptionPane.UNINITIALIZED_VALUE) {
                //ignore reset
                return;
            }
 
            //Reset the JOptionPane's value.
            //If you don't do this, then if the user
            //presses the same button next time, no
            //property change event will be fired.
            optionPane.setValue(
                    JOptionPane.UNINITIALIZED_VALUE);
 
            if (btnString1.equals(value)) {
                    typedText = textField.getText();
               // String ucText = typedText; //.toUpperCase();
                try {
                 if (foundNetwork()) {
                    //we're done; clear and dismiss the dialog
                    clearAndHide();
                 } else {
                    //text was invalid
                    textField.selectAll();
                    JOptionPane.showMessageDialog(
                    				ImportNetworkDialog.this,
                                    "Sorry, we can't find network \"" + typedText + "\"\n",
                                    "Try again",
                                    JOptionPane.ERROR_MESSAGE);
                    typedText = null;
                    textField.requestFocusInWindow();
                 }
                } catch (IOException | NdexException ex) {
                	JOptionPane.showMessageDialog(
            				ImportNetworkDialog.this,
                            "Sorry, " + ex.getMessage() + "\n",
                            "Try again",
                            JOptionPane.ERROR_MESSAGE);
            typedText = null;
            textField.requestFocusInWindow();
                }
            } else { //user closed dialog or clicked cancel
           /*     dd.setLabel("It's OK.  "
                         + "We won't force you to type "
                         + magicWord + "."); */
                typedText = null;
                clearAndHide();
            }
        }
    }
 
    /** This method clears the dialog and hides it. */
    public void clearAndHide() {
        textField.setText(null);
        setVisible(false);
    }
    
    // check if typed text points to a valid network
    private boolean foundNetwork() throws IOException, NdexException {
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
				return s != null;
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
