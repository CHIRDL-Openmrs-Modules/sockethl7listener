/**
 * 
 */
package org.openmrs.module.sockethl7listener;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;

import ca.uhn.hl7v2.model.Message;

/**
 * @author tmdugan
 *
 */
public interface HL7PatientHandler
{
	public List<PersonAddress> getAddresses(Message message);
	
	public String getBirthplace(Message message);
	
	public Date getBirthdate(Message message);
	
	public String getCitizenship(Message message);
	
	public Date getDateChanged(Message message);
	
	public Date getDeathDate(Message message);
	
	public String getGender(Message message);
	
	public Set<PatientIdentifier> getIdentifiers(Message message);
	
	public String getNextOfKin(Message message);
	
	public String getMothersName(Message message);
	
	public String getTelephoneNumber(Message message);
		
	public Boolean isDead(Message message);
	
	public PersonName getPatientName(Message message);
	
	public String getRace(Message message);
	
	public String[] getPatientIdentifierList(Message message);

	public String getAccountNumber(Message message); // DWE CHICA-406
	
	public String getEthnicity(Message message); // DWE CHICA-706
}
