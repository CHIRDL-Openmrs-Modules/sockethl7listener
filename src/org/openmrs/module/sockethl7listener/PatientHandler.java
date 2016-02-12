package org.openmrs.module.sockethl7listener;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.chirdlutil.util.ChirdlUtilConstants;

import ca.uhn.hl7v2.model.Message;

public class PatientHandler
{

	protected static  String ATTRIBUTE_NEXT_OF_KIN = "Mother's Name";
	protected static final String ATTRIBUTE_TELEPHONE = "Telephone Number";
	protected static final String ATTRIBUTE_RACE = "Race";
	protected static final String ATTRIBUTE_BIRTHPLACE = "Birthplace";
	protected static final String CITIZENSHIP = "Citizenship";
	protected static final String MATCH_INFO = "Other Matching Information";

	protected PatientService patientService;
	protected PersonService personService;

	protected static final Logger logger = Logger
			.getLogger("SocketHandlerLogger");
	
	public PatientHandler()
	{
		patientService = Context.getPatientService();
		personService = Context.getPersonService();
	}

	public Patient setPatientFromHL7(Message message, Date encounterDate,
			Location encounterLocation, HL7PatientHandler hl7PatientHandler)
	{
		Patient hl7Patient = new Patient();

		// Patient identifiers (MRNs)
		setIdentifiers(message, hl7Patient, encounterLocation, encounterDate,
				hl7PatientHandler);

		// Patient Name/ person name
		setPatientName(message, hl7Patient, encounterDate, hl7PatientHandler);

		// Race
		setRace(message, hl7Patient, encounterDate, hl7PatientHandler);

		// next of kin
		addAttribute(hl7Patient, ChirdlUtilConstants.PERSON_ATTRIBUTE_NEXT_OF_KIN, hl7PatientHandler
						.getNextOfKin(message), encounterDate);
				
		// birthdate
		hl7Patient.setBirthdate(hl7PatientHandler.getBirthdate(message));

		// birthplace
		addAttribute(hl7Patient, ATTRIBUTE_BIRTHPLACE, hl7PatientHandler
				.getBirthplace(message), encounterDate);

		// gender
		hl7Patient.setGender(hl7PatientHandler.getGender(message));

		// addresses
		setAddresses(message, hl7Patient, encounterDate, hl7PatientHandler);

		// telephone number
		addAttribute(hl7Patient, ATTRIBUTE_TELEPHONE, hl7PatientHandler
				.getTelephoneNumber(message), encounterDate);

		// citizenship
		addAttribute(hl7Patient, CITIZENSHIP, hl7PatientHandler
				.getCitizenship(message), encounterDate);

		// death date
		hl7Patient.setDeathDate(hl7PatientHandler.getDeathDate(message));

		// dead flag
		hl7Patient.setDead(hl7PatientHandler.isDead(message));

		// Last Update date/time
		Date lastUpdate = hl7PatientHandler.getDateChanged(message);
		if (lastUpdate != null)
		{
			hl7Patient.setDateChanged(lastUpdate);
		}

		// DWE CHICA-406
		// Patient account number
		setAccountNumber(message, hl7Patient, encounterDate, hl7PatientHandler);
		
		return hl7Patient;
	}

	protected void setIdentifiers(Message message, Patient hl7Patient,
			Location encounterLocation, Date encounterDate,
			HL7PatientHandler hl7PatientHandler)
	{

		try
		{
			Set<PatientIdentifier> identifiers = hl7PatientHandler
					.getIdentifiers(message);
			for (PatientIdentifier currIdent : identifiers)
			{
				currIdent.setLocation(encounterLocation);
				currIdent.setDateCreated(encounterDate);
				currIdent.setCreator(Context.getAuthenticatedUser());
				currIdent.setPatient(hl7Patient);
			}
			hl7Patient.setIdentifiers(identifiers);

		} catch (APIException e)
		{
			logger.error("Error setting patient identifiers. ", e);
		}
	}

	/**
	 * Sets person name and patient name
	 * 
	 * @param xpnList
	 */
	protected void setPatientName(Message message, Patient hl7Patient,
			Date encounterDate, HL7PatientHandler hl7PatientHandler)
	{
		PersonName name = hl7PatientHandler.getPatientName(message);
		name.setPerson(hl7Patient);

		hl7Patient.addName(name);

		name.setDateCreated(encounterDate);
		name.setCreator(Context.getAuthenticatedUser());

	}

	/**
	 * Set race based on integer code in hl7 PID message
	 * 
	 * @param ceRace
	 */
	protected void setRace(Message message, Patient hl7Patient,
			Date encounterDate, HL7PatientHandler hl7PatientHandler)
	{
		int raceID = 0;
		String race = "";

		try
		{
			raceID = Integer.parseInt(hl7PatientHandler.getRace(message));
		} catch (NumberFormatException e1)
		{
			logger
					.warn("Warning: Race ID from the PID could not be converted to integer. The value from hl7 was probably null, so default of 0 was used");
			raceID = 0;
		} catch (RuntimeException e1)
		{
			logger
					.debug("Warning: Race information not available in PID segment.");
		}

		// Set values based on NBS specific codes
		switch (raceID)
		{

		case 1:
			race = "WHITE";
			break;
		case 2:
			race = "BLACK";
			break;
		case 3:
			race = "AMERICAN INDIAN";
			break;
		case 4:
			race = "ASIAN";
			break;
		case 5:
			race = "OTHER";
			break;
		default:
			race = "";
			break;
		}

		// personAttrList += ";race:" + race ;

		addAttribute(hl7Patient, ATTRIBUTE_RACE, race, encounterDate);

	}

	/**
	 * Construct the resolved address.
	 * 
	 * @param xad
	 */
	protected void setAddresses(Message message, Patient hl7Patient,
			Date encounterDate, HL7PatientHandler hl7PatientHandler)
	{
		// create_date for the address is the encounter date
		try
		{
			List<PersonAddress> addresses = hl7PatientHandler
					.getAddresses(message);
			for (PersonAddress address : addresses)
			{
				hl7Patient.addAddress(address);
				address.setDateCreated(encounterDate);
				address.setCreator(Context.getAuthenticatedUser());
			}

		} catch (RuntimeException e)
		{
			logger.error("Error parsing address: " + e.getMessage());
			logger.error(e.getStackTrace());
		}

	}

	protected void addAttribute(Patient hl7Patient, String attributeTypeName,
			String value, Date encounterDate)
	{
		if(value == null){
			return;
		}
		PersonAttributeType attributeType = personService
				.getPersonAttributeTypeByName(attributeTypeName);

		if (attributeType == null)
		{
			attributeType = createAttributeType(attributeTypeName);
			if (attributeType == null){
				logger.error("Unable to create attribute: " + attributeTypeName);
				return;
			}
		}
		
		PersonAttribute attr = new PersonAttribute(attributeType, value);
		attr.setDateCreated(encounterDate);
		attr.setCreator(Context.getAuthenticatedUser());
		hl7Patient.addAttribute(attr);
	}

	/**
	 * Create a new attribute type - (patient matching string, mother's name,
	 * etc)
	 * 
	 * @param patString
	 * @return
	 */
	private PersonAttributeType createAttributeType(String patString)
	{
		PersonAttributeType personAttr = new PersonAttributeType();
		try
		{
			personAttr.setName(patString);
			personAttr.setFormat("java.lang.String");
			personAttr.setDescription(patString);
			personAttr.setSearchable(true);
			personService.savePersonAttributeType(personAttr);
		} catch (RuntimeException e)
		{
			logger.error("Unable to create new attribute type:" + patString);
			logger.error(e.getStackTrace());
		}
		return personAttr;

	}
	
	/**
	 * DWE CHICA-406 
	 * @param message
	 * @param hl7Patient
	 * @param encounterDate
	 * @param hl7PatientHandler
	 */
	protected void setAccountNumber(Message message, Patient hl7Patient,
			Date encounterDate, HL7PatientHandler hl7PatientHandler)
	{
		// Intentionally left empty
	}
}
