package org.cytoscape.cyndex2.internal;

import java.io.File;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.cyndex2.internal.rest.errors.ErrorBuilder;
import org.cytoscape.service.util.CyServiceRegistrar;

public class CyServiceModule {
	
	public static CyServiceModule INSTANCE = new CyServiceModule();
	
	private CyServiceRegistrar registrar;
	private ErrorBuilder errorBuilder;

	private CyServiceModule() {
		
	}
	public static void setServiceRegistrar(CyServiceRegistrar registrar) {
		INSTANCE.registrar = registrar;
	}
	public static <T> T getService(Class<T> clz) {
		return INSTANCE.registrar.getService(clz);
	}
	public static void setErrorBuilder(ErrorBuilder errorBuilder) {
		INSTANCE.errorBuilder = errorBuilder;
	}
	
	public ErrorBuilder getErrorBuilder() {
		return errorBuilder;
	}
	
	public CyApplicationConfiguration getConfig() {
		return registrar.getService(CyApplicationConfiguration.class);
	}
	
	public File getConfigDir() {
		return getConfig().getAppConfigurationDirectoryLocation(CyActivator.class);
	}
}
