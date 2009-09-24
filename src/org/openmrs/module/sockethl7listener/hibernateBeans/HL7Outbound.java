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
	
	public Date getDateProcessed() {
		return dateProcessed;
	}
	
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
	
	
	

}

