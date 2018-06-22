/*
 * Copyright (c) 2014, the Cytoscape Consortium and the Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.cytoscape.cyndex2.internal.singletons;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;

import org.cytoscape.cyndex2.internal.util.StringResources;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;


public enum NetworkManager
{
    INSTANCE;
//    private NetworkSummary selectedNetworkSummary;
    
//    private final Map<Long, CXInfoHolder> cxNetworkInfoTable;
    
    NetworkManager() { 

    }
    
    public void setCXInfoHolder(CyNetwork network, CXInfoHolder cxInfoHolder) {
    	// write values to network hidden tables
    }
    
    private void createColumnIfNotExists(CyTable table, String name, Class<?> clz) {
    	if (table.getColumn(name) == null) {
    		table.createColumn(name, clz, false);
    	}
    }
    
    public void addNetworkUUID (CyNetwork network, UUID networkId) {
    	CyTable net_table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
    	createColumnIfNotExists(net_table, StringResources.UUID_COLUMN, String.class);
    	CyRow row = net_table.getRow(network.getSUID());
    	row.set(StringResources.UUID_COLUMN, networkId.toString());
    }
    
    public UUID getNdexNetworkId(CyNetwork network) {
    	CyTable net_table = network.getTable(CyNetwork.class, CyNetwork.HIDDEN_ATTRS);
    	createColumnIfNotExists(net_table, StringResources.UUID_COLUMN, String.class);
    	CyRow row = net_table.getRow(network.getSUID());
    	String uuidStr = row.get(StringResources.UUID_COLUMN, String.class);
    	if (uuidStr == null) {
    		return null;
    	}
    	return UUID.fromString(uuidStr);
    }
    
}
