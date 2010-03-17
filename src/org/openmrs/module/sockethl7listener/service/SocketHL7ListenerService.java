package org.openmrs.module.sockethl7listener.service;

import java.util.Date;

import org.openmrs.Encounter;
import org.openmrs.module.sockethl7listener.hibernateBeans.HL7Outbound;
import org.openmrs.module.sockethl7listener.hibernateBeans.PatientMessage;
import org.springframework.transaction.annotation.Transactional;


/**
 * Defines services used by this module
 *  
 * @author Tammy Dugan
 *
 */
@Transactional
public interface SocketHL7ListenerService
{
	public Integer saveMessageToDatabase(Encounter enc, String encodedMessage, 
			Date ackDate, Integer port, String host);
	
	public HL7Outbound saveMessageToDatabase(HL7Outbound hl7Out);

	public void setHl7Message(Integer pid, Integer encounter_id,  String message, boolean dup_string,
			boolean dup_enc, Integer port);

	public String getNPI(String firstName, String lastName);
	
	public boolean checkMD5(String incoming, Integer port);
	
	public void messageProcessed(Encounter encounter);
	
	public String getFaxNumber(String firstName, String lastName);

	public PatientMessage getPatientMessageByEncounter(Integer encounterId);
}