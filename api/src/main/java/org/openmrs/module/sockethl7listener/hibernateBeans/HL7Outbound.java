package org.openmrs.module.sockethl7listener.hibernateBeans;


import java.io.Serializable;
import java.util.Date;

import org.openmrs.Encounter;

public class HL7Outbound implements Serializable {

	private static final long serialVersionUID = 8882704913734764446L;

	private Integer HL7OutQueueId;
	private String hl7Message;
	private Date dateProcessed;
	private Encounter encounter;
	private Date ackReceived;
	private Integer port;
	
	private String host;
	
	
	public Integer getHL7OutQueueId() {
		return HL7OutQueueId;
	}
	public void setHL7OutQueueId(Integer outQueueId) {
		this.HL7OutQueueId = outQueueId;
	}
	public String getHl7Message() {
		return hl7Message;
	}
	public void setHl7Message(String hl7Message) {
		this.hl7Message = hl7Message;
		this.dateProcessed = new Date();
	}
	public Encounter getEncounter() {
		return encounter;
	}
	public void setEncounter(Encounter encounter) {
		this.encounter = encounter;
	}

	public void setDateProcessed(Date dateProcessed) {
		this.dateProcessed = dateProcessed;
	}

	public Date getAckReceived() {
		return ackReceived;
	}

	public void setAckReceived(Date ackReceived) {
		this.ackReceived = ackReceived;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(Integer port) {
		this.port = port;
	}
	
	public Integer getPort() {
		return port;
	}


	public Date getDateProcessed() {
		return dateProcessed;
	}
	
	
	

}

