package org.cytoscape.cyndex2.internal;

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
	
	public static final <S> S getService(Class<S> serviceClass, String filter) {
		return INSTANCE.registrar != null ? INSTANCE.registrar.getService(serviceClass, filter) : null;
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
	
	
}
