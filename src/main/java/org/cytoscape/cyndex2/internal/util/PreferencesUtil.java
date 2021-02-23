/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cytoscape.cyndex2.internal.util;

import java.util.Properties;

import org.cytoscape.cyndex2.internal.CyServiceModule;
import org.cytoscape.property.CyProperty;

/**
 *
 * @author wilderkrieger
 */
public class PreferencesUtil {
    public static final String VIEW_THRESHOLD = "viewThreshold";	
    
    public static String getProperty(String key) {
		final Properties props = (Properties) CyServiceModule.getService(CyProperty.class, "(cyPropertyName=cytoscape3.props)").getProperties();
		return props.getProperty(key);
	}
	
	
	public static Integer getIntegerProperty(String key, Integer defaultValue) {
		final String property = getProperty(key);

		try {
			return Integer.parseInt(property);
		} catch (Exception e) {
			return defaultValue;
		}
	}
}
