package org.openmrs.module.sockethl7listener.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.hibernate.HibernateException;
import org.openmrs.Encounter;
import org.openmrs.module.sockethl7listener.hibernateBeans.HL7Outbound;
import org.openmrs.module.sockethl7listener.hibernateBeans.PatientMessage;


/**
 * Defines services used by this module
 *  
 * @author Tammy Dugan
 *
 */
public interface SocketHL7ListenerService
{
	public Integer saveMessageToDatabase(Encounter enc, String encodedMessage, 
			Date ackDate, Integer port, String host);
	
	public HL7Outbound saveMessageToDatabase(HL7Outbound hl7Out);

	public void setHl7Message(Integer pid, Integer encounter_id,  String message, boolean dup_string,
			boolean dup_enc, Integer port);

	public String getNPI(String firstName, String lastName);
	
	public boolean checkMD5(String incoming, Integer port);
	
	public void messageProcessed(Encounter encounter,HashMap<String,Object> parameters);
	
	public String getFaxNumber(String firstName, String lastName);

	public PatientMessage getPatientMessageByEncounter(Integer encounterId);
	
	/**
	 * DWE CHICA-636
	 * Get a list of hl7_out_queue records that are waiting to be sent
	 * @param host
	 * @param port
	 * @return list of HL7Outbound objects
	 */
	public List<HL7Outbound> getPendingHL7OutboundByHostAndPort(String host, Integer port) throws HibernateException; // DWE CHICA-636
}