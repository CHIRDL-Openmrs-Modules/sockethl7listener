package org.openmrs.module.sockethl7listener.impl;

import java.security.DigestException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.HibernateException;
import org.openmrs.Encounter;
import org.openmrs.module.chirdlutil.util.Util;
import org.openmrs.module.sockethl7listener.db.SocketHL7ListenerDAO;
import org.openmrs.module.sockethl7listener.hibernateBeans.HL7Outbound;
import org.openmrs.module.sockethl7listener.hibernateBeans.PatientMessage;
import org.openmrs.module.sockethl7listener.service.SocketHL7ListenerService;

/**
 * Defines implementations of services used by this module
 * 
 * @author Meena Sheley and Tammy Dugan
 *
 */
public class SocketHL7ListenerServiceImpl implements SocketHL7ListenerService{
	 
	private static final Logger log = LoggerFactory.getLogger("SocketHandlerLogger");

	private SocketHL7ListenerDAO dao;

	/**
	 * Empty constructor
	 */
	public SocketHL7ListenerServiceImpl()
	{
	}

	/**
	 * @return SocketHL7ListenerDAO
	 */
	public SocketHL7ListenerDAO getSocketHL7ListenerDAO()
	{
		return this.dao;
	}

	/**
	 * Sets the DAO for this service. The dao
	 * allows interaction with the database.
	 * @param dao
	 */
	public void setSocketHL7ListenerDAO(SocketHL7ListenerDAO dao)
	{
		this.dao = dao;
	}
	
	public Integer saveMessageToDatabase(Encounter enc, String encodedMessage, Date ackDateTime, Integer port, String host)
	{
		Integer hl7QueueId = null;
		HL7Outbound hl7b = new HL7Outbound();
		
		hl7b.setHl7Message(encodedMessage);
		hl7b.setEncounter(enc);
		hl7b.setAckReceived(ackDateTime);
		hl7b.setPort(port);
		hl7b.setHost(host);
		
		getSocketHL7ListenerDAO().saveHL7Outbound(hl7b);
		return hl7QueueId;
	}
	
	public HL7Outbound saveMessageToDatabase(HL7Outbound hl7Out)
	{
		return getSocketHL7ListenerDAO().saveHL7Outbound(hl7Out);
	}

	public void setHl7Message(Integer pid, Integer encounter_id, String message, boolean dup_string,
			boolean dup_enc, Integer port)
	{
		PatientMessage pm = new PatientMessage();
		if(message == null){
			return;
		}
		try
		{

			pm.setDuplicateDatetime(dup_enc);
			pm.setDuplicateString(dup_string);
			pm.setHl7Message(message);
			pm.setPatient_id(pid);
			pm.setDateCreated(new Date());
			pm.setEncounter_id(encounter_id);
			pm.setHl7source(port.toString());
			int index = message.indexOf("PID");
			if(index >=0){
				pm.setMd5(Util.computeMD5(message.substring(index)));
			}
			getSocketHL7ListenerDAO().savePatientMessage(pm);
			
		} catch (Exception e)
		{
			log.error("Error saving hl7 message for encounter id {}.", encounter_id, e);
		}
		
		return;
	}

	public boolean checkMD5(String incoming, Integer port)
	{
		boolean duplicate = false;
		try
		{
			List<PatientMessage> hl7Messages = getSocketHL7ListenerDAO().checkMD5(incoming);

			Iterator <PatientMessage> it = hl7Messages.iterator();
			if (it.hasNext()){
				PatientMessage pm = it.next();
				duplicate = true;
				log.error("Duplicate message for patient: {} encounter: {}", pm.getPatient_id(), pm.getEncounter_id());
				
				setHl7Message(pm.getPatient_id(), pm.getEncounter_id(), 
							incoming, duplicate, duplicate, port);
			}

		} catch (HibernateException e)
		{
			log.error("Error saving hl7 message after MD5 check.",e);
		} catch (RuntimeException e)
		{
			log.error("Exception checking for MD5.", e);
		}

		return duplicate;
	}
	
	public void messageProcessed(Encounter encounter, HashMap<String, Object> parameters) {
		// nothing is done here.  Modules override/hook on this method
	}
	
	public PatientMessage getPatientMessageByEncounter(Integer encounterId){
		return getSocketHL7ListenerDAO().getPatientMessageByEncounter(encounterId);
	}

	/**
	 * DWE CHICA-636
	 * @see org.openmrs.module.sockethl7listener.service.SocketHL7ListenerService#getPendingHL7OutboundByHostAndPort(String, Integer)
	 */
	@Override
	public List<HL7Outbound> getPendingHL7OutboundByHostAndPort(String host, Integer port) throws HibernateException
	{
		return getSocketHL7ListenerDAO().getPendingHL7OutboundByHostAndPort(host, port);
	}
}