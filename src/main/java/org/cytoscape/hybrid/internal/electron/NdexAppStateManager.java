package org.cytoscape.hybrid.internal.electron;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.events.CyShutdownEvent;
import org.cytoscape.application.events.CyShutdownListener;
import org.cytoscape.hybrid.internal.login.Credential;
import org.cytoscape.hybrid.internal.login.LoginManager;
import org.cytoscape.hybrid.internal.login.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NdexAppStateManager implements CyShutdownListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(NdexAppStateManager.class);
	
	private static final String FORMAT_VERSION = "1.0";
	
	// Config file name stored in the CytoscapeConfiguration dir. 
	public static final String CONFIG_FILE = "ndex-config.json";
	public static final String KEY_FILE = "ndex-key";
	
	private final ObjectMapper mapper;
	private final File configFile;
	
	private final PasswordUtil passUtil;
	private SecretKey key;
	
	// The state of the app
	private NdexAppState appState;
	
	private final LoginManager loginManager;
	
	public NdexAppStateManager(final CyApplicationConfiguration appConfig, final LoginManager loginManager) {
		this.passUtil = new PasswordUtil();
		
		this.mapper = new ObjectMapper();
		this.loginManager = loginManager;
		final File configLocation = appConfig.getConfigurationDirectoryLocation();
		this.configFile = new File(configLocation, CONFIG_FILE);
		final File keyFile = new File(configLocation, KEY_FILE);
		restoreKey(keyFile);
		
		this.appState = restoreState();
		
		setLoginState();
	}
	
	private final void restoreKey(final File keyFile) {
		if(keyFile.exists()) {
			try {
				this.key = passUtil.loadKey(keyFile);
				
			} catch (ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			key = passUtil.generateKey();
			try {
				passUtil.storeKey(key, keyFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private final void setLoginState() {
		final String currentServer = appState.getSelectedServerName();
		if(currentServer != null) {
			final Credential credential = appState.getServers().get(currentServer);
			loginManager.setLogin(credential);
		}
	}
	
	private final NdexAppState createInitialState() {
		final NdexAppState state = new NdexAppState();
		state.setFormatVersion(FORMAT_VERSION);
		
		final Map<String, Credential> servers = new HashMap<>();
		state.setServers(servers);
		return state;
	}

	private final NdexAppState restoreState() {
		System.out.println("Loading from Config...");
		if(configFile.exists()) {
			// Load it into object
			try {
				final NdexAppState state = mapper.readValue(configFile, NdexAppState.class);		
				System.out.println("* App State Restored:");
				System.out.println(mapper.writeValueAsString(state));
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

	@Override
	public void handleEvent(CyShutdownEvent evt) {
		System.out.println("@@ Saving Ndex App state...");
		
		final Credential currentLogin = loginManager.getLogin();
		if(currentLogin != null) {
			final String selectedServerName = currentLogin.getServerName();
			appState.setSelectedServerName(selectedServerName);
		}
		appState.setServers(loginManager.getServers());
		
		try {
			final Map<String, Credential> servers = encodeServerPassword();
			appState.setServers(servers);
			System.out.println("#### New server str:");
			System.out.println(mapper.writeValueAsString(servers));
			
			System.out.println(mapper.writeValueAsString(appState));
			mapper.writeValue(configFile, appState);
			System.out.println("* NDEx States saved!");
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.warn("Failed to save states.", e);
		}
	}
	
	private final Map<String, Credential> encodeServerPassword() {
		final Map<String, Credential> servers = appState.getServers();
		System.out.println("#### ENCODING: " + servers.size());
		for(String name: servers.keySet()) {
			encode(servers.get(name));
		}
		
		return servers;
	}
	
	private final void encode(final Credential server) {
		System.out.println("* Server: " + server.getUserName());
		System.out.println("* Server PW: " + server.getUserPass());
		final String pw = server.getUserPass();
		if(pw == null) {
			return;
		}
		
		try {
			System.out.println(mapper.writeValueAsString(server));
		} catch (JsonProcessingException e1) {
			e1.printStackTrace();
		}
		
		try {
			final String encoded = passUtil.encode(server.getUserPass(), key);
			System.out.println("* ENCODE: " + encoded);
			server.setUserPass(encoded);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("#### PW:" + server.getUserPass());
	}
}
