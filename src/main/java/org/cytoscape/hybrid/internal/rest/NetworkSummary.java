package org.cytoscape.hybrid.internal.rest;

import java.util.Map;

public class NetworkSummary {
	
	private Long suid;
	private String name;
	private String ndexUuid;
	private Map<String, ?> props;
	
	
	public Long getSuid() {
		return suid;
	}
	public void setSuid(Long suid) {
		this.suid = suid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getNdexUuid() {
		return ndexUuid;
	}
	public void setNdexUuid(String ndexUuid) {
		this.ndexUuid = ndexUuid;
	}
	public Map<String, ?> getProps() {
		return props;
	}
	public void setProps(Map<String, ?> props) {
		this.props = props;
	}
}
