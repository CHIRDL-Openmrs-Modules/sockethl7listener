package org.openmrs.module.sockethl7listener.db.hibernate;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.openmrs.module.chirdlutil.util.Util;
import org.openmrs.module.sockethl7listener.db.SocketHL7ListenerDAO;
import org.openmrs.module.sockethl7listener.hibernateBeans.HL7Outbound;
import org.openmrs.module.sockethl7listener.hibernateBeans.PatientMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Hibernate implementations of SocketHL7Listener related database functions.
 * 
 * @author Meena Sheley and Tammy Dugan
 */
public class HibernateSocketHL7ListenerDAO implements SocketHL7ListenerDAO{

	private static final Logger log =  LoggerFactory.getLogger("SocketHandlerLogger");
	
	/**
	 * Hibernate session factory
	 */
	private SessionFactory sessionFactory;

	/**
	 * Empty constructor
	 */
	public HibernateSocketHL7ListenerDAO(){
		//empty constructor
	}

	/**
	 * Set session factory
	 * 
	 * @param sessionFactory
	 */
	public void setSessionFactory(SessionFactory sessionFactory){
		this.sessionFactory = sessionFactory;
	}

	public HL7Outbound saveHL7Outbound(HL7Outbound hl7Outbound){
		this.sessionFactory.getCurrentSession().saveOrUpdate(hl7Outbound);
		return hl7Outbound;
	}

	public PatientMessage savePatientMessage(PatientMessage patientMessage){
		this.sessionFactory.getCurrentSession().save(patientMessage);
		return patientMessage;
	}
	
	public List<PatientMessage> checkMD5(String incoming){
		String sqlSelect = "SELECT * from sockethl7listener_patient_message "
		+ " where md5 = :incomingMD5"; 
		
		try{
			String incomingMD5 = null;
			int index = incoming.indexOf("PID");
			if(index >=0){
				incomingMD5 = Util.computeMD5(incoming.substring(index));
			}
			
			SQLQuery query = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sqlSelect);
			query.setString("incomingMD5", incomingMD5);
			query.addEntity(PatientMessage.class);
			return query.list();
		} catch (Exception e){
			log.error("Error getting PatientMessage by MD5 string.",e);
		}
		return null;
	}
	
	public PatientMessage getPatientMessageByEncounter(Integer encounterId){
		String sqlSelect = "SELECT * from sockethl7listener_patient_message "
		+ " where encounter_id = :encounterId and duplicate_string=0 and duplicate_datetime=0"; 
		
		try{
			SQLQuery query = this.sessionFactory.getCurrentSession()
					.createSQLQuery(sqlSelect);
			query.setInteger("encounterId", encounterId);
			query.addEntity(PatientMessage.class);
			return (PatientMessage) query.uniqueResult();
		} catch (Exception e){
			log.error("Error getting patient hl7 message by encounter. EncounterId:  {}", encounterId, e);
		}
		return null;
	}
	
	/**
	 * DWE CHICA-636
	 * @see org.openmrs.module.sockethl7listener.db.SocketHL7ListenerDAO#getPendingHL7OutboundByHostAndPort(String, Integer)
	 */
	@Override
	public List<HL7Outbound> getPendingHL7OutboundByHostAndPort(String host, Integer port) throws HibernateException{
		Criteria criteria = sessionFactory.getCurrentSession().createCriteria(HL7Outbound.class)
				.add(Restrictions.eq("host", host))
				.add(Restrictions.eq("port", port))
				.add(Restrictions.isNull("ackReceived"));
		
		return criteria.list();
	}
}
