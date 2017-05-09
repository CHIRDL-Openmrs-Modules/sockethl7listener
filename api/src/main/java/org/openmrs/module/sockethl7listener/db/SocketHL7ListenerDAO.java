package org.openmrs.module.sockethl7listener.db;

import java.util.List;

import org.hibernate.HibernateException;
import org.openmrs.module.sockethl7listener.hibernateBeans.HL7Outbound;
import org.openmrs.module.sockethl7listener.hibernateBeans.NPI;
import org.openmrs.module.sockethl7listener.hibernateBeans.PatientMessage;
import org.springframework.transaction.annotation.Transactional;

/**
 * SocketHL7Listener related database functions
 * 
 * @author Tammy Dugan
 */
@Transactional
public interface SocketHL7ListenerDAO {

	public HL7Outbound saveHL7Outbound(HL7Outbound hl7Outbound);

	public PatientMessage savePatientMessage(PatientMessage patientMessage);

	public List<NPI> getNPIByName(String firstName, String lastName);
	
	public List<PatientMessage> checkMD5(String incoming);
	
	public PatientMessage getPatientMessageByEncounter(Integer encounterId);
	
	/**
	 * DWE CHICA-636
	 * Get a list of hl7_out_queue records that are waiting to be sent
	 * @param host
	 * @param port
	 * @return list of HL7Outbound objects
	 */
	public List<HL7Outbound> getPendingHL7OutboundByHostAndPort(String host, Integer port) throws HibernateException;
}
