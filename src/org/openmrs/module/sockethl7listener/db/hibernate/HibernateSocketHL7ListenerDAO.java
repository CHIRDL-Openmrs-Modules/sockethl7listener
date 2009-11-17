package org.openmrs.module.sockethl7listener.db.hibernate;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.openmrs.module.sockethl7listener.db.SocketHL7ListenerDAO;
import org.openmrs.module.sockethl7listener.hibernateBeans.HL7Outbound;
import org.openmrs.module.sockethl7listener.hibernateBeans.NPI;
import org.openmrs.module.sockethl7listener.hibernateBeans.PatientMessage;
import org.openmrs.module.sockethl7listener.util.Util;

/**
 * Hibernate implementations of SocketHL7Listener related database functions.
 * 
 * @author Meena Sheley and Tammy Dugan
 */
public class HibernateSocketHL7ListenerDAO implements SocketHL7ListenerDAO
{
	protected final Log log = LogFactory.getLog(getClass());

	/**
	 * Hibernate session factory
	 */
	private SessionFactory sessionFactory;

	/**
	 * Empty constructor
	 */
	public HibernateSocketHL7ListenerDAO()
	{
	}

	/**
	 * Set session factory
	 * 
	 * @param sessionFactory
	 */
	public void setSessionFactory(SessionFactory sessionFactory)
	{
		this.sessionFactory = sessionFactory;
	}

	public HL7Outbound saveHL7Outbound(HL7Outbound hl7Outbound)
	{
		this.sessionFactory.getCurrentSession().saveOrUpdate(hl7Outbound);
		return hl7Outbound;
	}

	public void savePatientMessage(PatientMessage patientMessage)
	{
		this.sessionFactory.getCurrentSession().save(patientMessage);
	}

	public List<NPI> getNPIByName(String firstName, String lastName)
	{
		String sqlSelect = "SELECT * "
				+ " from sockethl7listener_npi  WHERE NPI_LN = :ln and NPI_FN = :fn";
		SQLQuery query = this.sessionFactory.getCurrentSession()
				.createSQLQuery(sqlSelect);
		query.setString("ln", lastName);
		query.setString("fn", firstName);
		query.addEntity(NPI.class);

		return query.list();
	}
	
	public List<PatientMessage> checkMD5(String incoming)
	{
		String sqlSelect = "SELECT * from sockethl7listener_patient_message "
		+ " where md5  = ?"; 
		
		try
		{
			String incomingMD5 = null;
			int index = incoming.indexOf("PID");
			if(index >=0)
			{
				incomingMD5 = Util.computeMD5(incoming.substring(index));
			}
			
			SQLQuery query = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sqlSelect);
			query.setString(0, incomingMD5);
			query.addEntity(PatientMessage.class);
			return query.list();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	public PatientMessage getPatientMessageByEncounter(Integer encounterId)
	{
		String sqlSelect = "SELECT * from sockethl7listener_patient_message "
		+ " where encounter_id  = ? and duplicate_string=0 and duplicate_datetime=0"; 
		
		try
		{
			SQLQuery query = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sqlSelect);
			query.setInteger(0, encounterId);
			query.addEntity(PatientMessage.class);
			return (PatientMessage) query.uniqueResult();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
}
