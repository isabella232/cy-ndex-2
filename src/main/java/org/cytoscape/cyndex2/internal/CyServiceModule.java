package org.cytoscape.cyndex2.internal;

import org.cytoscape.service.util.CyServiceRegistrar;

public class CyServiceModule {
	
	public static CyServiceModule INSTANCE = new CyServiceModule();
	private CyServiceRegistrar registrar;
	
	private CyServiceModule() {
		
	}
	public static void setServiceRegistrar(CyServiceRegistrar registrar) {
		INSTANCE.registrar = registrar;
	}
	public static <T> T getService(Class<T> clz) {
		return INSTANCE.registrar.getService(clz);
	}
	
	
}
