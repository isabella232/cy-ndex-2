package org.cytoscape.hybrid.internal.electron;

import java.io.File;
import java.io.IOException;

import javax.crypto.SecretKey;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.events.CyShutdownEvent;
import org.cytoscape.application.events.CyShutdownListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class NdexAppStateManager implements CyShutdownListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NdexAppStateManager.class);
	
	private static final String FORMAT_VERSION = "1.0";
	
	// Config file name stored in the CytoscapeConfiguration dir. 
	public static final String CONFIG_FILE = "ndex-config.json";
	public static final String KEY_FILE = "ndex-key";
	
	private final ObjectMapper mapper;
	private final File configFile;
	
	private SecretKey key;
	
	// The state of the app
	private NdexAppState appState;
	
	
	public NdexAppStateManager(final CyApplicationConfiguration appConfig) {
		this.mapper = new ObjectMapper();
		final File configLocation = appConfig.getConfigurationDirectoryLocation();
		this.configFile = new File(configLocation, CONFIG_FILE);
		this.appState = restoreState();
	}
	
	
	
	private final NdexAppState createInitialState() {
		final NdexAppState state = new NdexAppState();
		state.setFormatVersion(FORMAT_VERSION);
		return state;
	}

	private final NdexAppState restoreState() {
		if(configFile.exists()) {
			// Load it into object
			try {
				final NdexAppState state = mapper.readValue(configFile, NdexAppState.class);		
				return state;
			} catch (IOException e) {
				LOGGER.warn("Could not read config file.  Initial state will be used instead.", e);
				return createInitialState();
			}
		} else {
			System.out.println("No Config file found.");
			return createInitialState();
		}
	}
	
	/**
	 * For saving state to config directory.
	 */
	@Override
	public void handleEvent(CyShutdownEvent evt) {
	}
}
