package org.cytoscape.cyndex2.internal;

import org.cytoscape.ci.CIErrorFactory;
import org.cytoscape.ci.CIExceptionFactory;
import org.cytoscape.ci.CIResponseFactory;
import org.osgi.util.tracker.ServiceTracker;

public class CIServiceManager {
	private final ServiceTracker ciResponseFactoryTracker, ciErrorFactoryTracker, ciExceptionFactoryTracker;
	private CIResponseFactory ciResponseFactory;
	private CIExceptionFactory ciExceptionFactory;
	private CIErrorFactory ciErrorFactory;
	public CIServiceManager(final ServiceTracker ciResponseFactoryTracker, final ServiceTracker ciErrorFactoryTracker, final ServiceTracker ciExceptionFactoryTracker){
		this.ciErrorFactoryTracker = ciErrorFactoryTracker;
		this.ciExceptionFactoryTracker = ciExceptionFactoryTracker;
		this.ciResponseFactoryTracker = ciResponseFactoryTracker;
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
