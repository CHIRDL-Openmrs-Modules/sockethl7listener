package org.openmrs.module.sockethl7listener.impl;

import java.security.DigestException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.openmrs.Encounter;
import org.openmrs.module.sockethl7listener.db.SocketHL7ListenerDAO;
import org.openmrs.module.sockethl7listener.hibernateBeans.HL7Outbound;
import org.openmrs.module.sockethl7listener.hibernateBeans.NPI;
import org.openmrs.module.sockethl7listener.hibernateBeans.PatientMessage;
import org.openmrs.module.sockethl7listener.service.SocketHL7ListenerService;
import org.openmrs.module.sockethl7listener.util.Util;
import org.springframework.transaction.annotation.Transactional;

/**
 * Defines implementations of services used by this module
 * 
 * @author Meena Sheley and Tammy Dugan
 *
 */
@Transactional
public class SocketHL7ListenerServiceImpl implements SocketHL7ListenerService
{
	private static final Logger socketHandlerLogger = Logger.getLogger("SocketHandlerLogger");

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
	
	public void saveMessageToDatabase(Encounter enc, String encodedMessage)
	{
		HL7Outbound hl7b = new HL7Outbound();
		hl7b.setHl7Message(encodedMessage);
		hl7b.setEncounter(enc);
		
		getSocketHL7ListenerDAO().saveHL7Outbound(hl7b);
	}

	public void setHl7Message(int pid, int encounter_id, String message, boolean dup_string,
			boolean dup_enc, Integer port)
	{
		if(message == null){
			return;
		}
		try
		{

			PatientMessage pm = new PatientMessage();
			pm.setDuplicateDatetime(dup_enc);
			pm.setDuplicateString(dup_string);
			pm.setHl7Message(message);
			pm.setPatient_id(pid);
			pm.setDateCreated(new Date());
			pm.setEncounter_id(encounter_id);
			pm.setHl7source(port.toString());
			try
			{
				int index = message.indexOf("PID");
				if(index >=0)
				{
					pm.setMd5(Util.computeMD5(message.substring(index)));
				}
			} catch (DigestException e)
			{
			}
			getSocketHL7ListenerDAO().savePatientMessage(pm);
		} catch (HibernateException e)
		{
			socketHandlerLogger.error("Exception inserting hl7message. ", e);
		}
	}

	public String getNPI(String firstName, String lastName)
	{
		List<NPI> matchingNPIs = getSocketHL7ListenerDAO().getNPIByName(firstName, lastName);
		
		String npi = "";
		
		try
		{
			for(NPI currNPI:matchingNPIs)
			{
				String row = currNPI.getNpi();
				if (row != null)
				{
					npi = row;
				}
			}
		} catch (RuntimeException e)
		{

			e.printStackTrace();
		}

		return npi;
	}
	
	public String getFaxNumber(String firstName, String lastName)
	{
		List<NPI> matchingNPIs = getSocketHL7ListenerDAO().getNPIByName(firstName, lastName);
				
		if(matchingNPIs.size()>0)
		{
			NPI npi = matchingNPIs.get(0);
			return npi.getFaxNumber();
		}

		return null;
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
				socketHandlerLogger.warn("Duplicate message for patient=" + pm.getPatient_id() +
						"; encounterID= " + pm.getEncounter_id());
				
				setHl7Message(pm.getPatient_id(), pm.getEncounter_id(), 
							incoming, duplicate, duplicate, port);
			}

		} catch (HibernateException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return duplicate;
	}
	
	public void messageProcessed(Encounter encounter) {
		// nothing is done here.  Modules override/hook on this method
	}
}