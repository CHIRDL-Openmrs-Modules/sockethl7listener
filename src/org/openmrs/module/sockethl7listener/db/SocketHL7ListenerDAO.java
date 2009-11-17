package org.openmrs.module.sockethl7listener.db;

import java.util.List;

import org.openmrs.module.sockethl7listener.hibernateBeans.HL7Outbound;
import org.openmrs.module.sockethl7listener.hibernateBeans.NPI;
import org.openmrs.module.sockethl7listener.hibernateBeans.PatientMessage;


/**
 * SocketHL7Listener related database functions
 * 
 * @author Tammy Dugan
 */
public interface SocketHL7ListenerDAO {

	public HL7Outbound saveHL7Outbound(HL7Outbound hl7Outbound);

	public void savePatientMessage(PatientMessage patientMessage);

	public List<NPI> getNPIByName(String firstName, String lastName);
	
	public List<PatientMessage> checkMD5(String incoming);
	
	public PatientMessage getPatientMessageByEncounter(Integer encounterId);
}
