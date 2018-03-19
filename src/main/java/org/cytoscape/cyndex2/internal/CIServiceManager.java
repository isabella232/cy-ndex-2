package org.cytoscape.cyndex2.internal;

import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.ci.CIResponseFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

public class CIServiceManager {
	private final ServiceTracker ciResponseFactoryTracker, ciErrorFactoryTracker, ciExceptionFactoryTracker;
	private CIResponseFactory ciResponseFactory;
	private CIExceptionFactory ciExceptionFactory;
	private CIErrorFactory ciErrorFactory;
	public CIServiceManager(final BundleContext bc) throws InvalidSyntaxException{
		ciResponseFactoryTracker = new ServiceTracker(bc,
				bc.createFilter("(objectClass=org.cytoscape.ci.CIResponseFactory)"), null);
		ciResponseFactoryTracker.open();
		ciExceptionFactoryTracker = new ServiceTracker(bc,
				bc.createFilter("(objectClass=org.cytoscape.ci.CIExceptionFactory)"), null);
		ciExceptionFactoryTracker.open();
		ciErrorFactoryTracker = new ServiceTracker(bc,
				bc.createFilter("(objectClass=org.cytoscape.ci.CIErrorFactory)"), null);
		ciErrorFactoryTracker.open();
	}
	
	public CIResponseFactory getCIResponseFactory(){
		if (ciResponseFactory == null){
			ciResponseFactory = (CIResponseFactory) ciResponseFactoryTracker.getService();
		}
		return ciResponseFactory;
	}
	
	public CIErrorFactory getCIErrorFactory(){
		if (ciErrorFactory == null){
			ciErrorFactory = (CIErrorFactory) ciErrorFactoryTracker.getService();
		}
		return ciErrorFactory;
	}
	
	public CIExceptionFactory getCIExceptionFactory(){
		if (ciExceptionFactory == null){
			ciExceptionFactory = (CIExceptionFactory) ciExceptionFactoryTracker.getService();
		}
		return ciExceptionFactory;
	}
}
