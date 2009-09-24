package org.openmrs.module.sockethl7listener;

import java.util.Date;

import org.apache.log4j.Logger;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;

/**
 * Evaluates differences between hl7 patient and matched patient to define the
 * most accurate results.
 * 
 * @author msheley
 * 
 */
public class MatchHandler {
	
	private static final String PROVIDER_ID = "Provider ID";
	private static final String PROVIDER_NAME = "Provider Name";
	private static final String MATCH_INFO = "Other Matching Information";
	public static final String ATTRIBUTE_NEXT_OF_KIN = "Mother's Name";
	public static final String ATTRIBUTE_TELEPHONE = "Telephone Number";
	public static final String ATTRIBUTE_RACE = "Race";
	public static final String ATTRIBUTE_BIRTHPLACE = "Birthplace";
	private static final String PATIENT_IDENT_TYPE = "MRN";
	private static final Logger logger = Logger.getLogger("SocketHandlerLogger");
	private static final Logger matchLogger = Logger.getLogger("MatchResultsLogger");
	private static final Logger matchStatsLogger = Logger.getLogger("MatchStatsLogger");
	protected PatientIdentifier bestIdentifier;
	protected PersonName bestName;
	protected String correctGender;
	protected Date DOB;
	protected Provider bestProv;
	protected PersonAddress bestAddress;
	protected PersonAttribute bestNKAttribute;
	protected PersonAttribute bestTelephoneAttr;
	
	

	public MatchHandler() {
		
	}

	/**
	 * Resolve the differences between matched patient and hl7patient
	 * 
	 * @return resolvedPatient
	 */
	public Patient setPatient(Patient hl7Patient, Patient matchedPatient,
			Date encounterDate)
	{
		// Start with matched patient demographics and update with new hl7
		// demographics where applicable.
		Patient resolvedPatient = initializePatient(hl7Patient, matchedPatient);
		if (resolvedPatient == null){
			return null;
		}
		if (hl7Patient == null){
			return resolvedPatient;
		}
	
		bestIdentifier = getBestIdentifier(hl7Patient,
				resolvedPatient, encounterDate);
		if (bestIdentifier != null)
		{
			resolvedPatient.getPatientIdentifier().setPreferred(false);
			resolvedPatient.addIdentifier(bestIdentifier);
		} else
		{
			bestIdentifier = resolvedPatient.getPatientIdentifier();
		}

		bestName = getBestName(hl7Patient.getPersonName(), resolvedPatient.getPersonName(),
				encounterDate);
		if (bestName != null)
		{
			resolvedPatient.getPersonName().setPreferred(false);
			resolvedPatient.addName(bestName);
		} else
		{
			bestName = resolvedPatient.getPersonName();
		}

		correctGender = getBestGender(hl7Patient, resolvedPatient);
		resolvedPatient.setGender(correctGender);

		DOB = getBestDOB(hl7Patient, resolvedPatient);
		resolvedPatient.setBirthdate(DOB);


		bestAddress = getBestAddress(hl7Patient, resolvedPatient,
				encounterDate);
		if (bestAddress != null)
		{
			resolvedPatient.getPersonAddress().setPreferred(false);
			resolvedPatient.addAddress(bestAddress);
		} else
		{
			bestAddress = resolvedPatient.getPersonAddress();
		}

		bestNKAttribute = getBestNK(hl7Patient,
				resolvedPatient, encounterDate);
		if (bestNKAttribute != null)
		{
			if (resolvedPatient.getAttribute(ATTRIBUTE_NEXT_OF_KIN) != null)
			{
				resolvedPatient.getAttribute(ATTRIBUTE_NEXT_OF_KIN).setVoided(
						true);
			}
			resolvedPatient.addAttribute(bestNKAttribute);
		} else
		{
			bestNKAttribute = resolvedPatient
					.getAttribute(ATTRIBUTE_NEXT_OF_KIN);
		}

		
		bestTelephoneAttr = getBestTel(hl7Patient.getAttribute(ATTRIBUTE_TELEPHONE),
				resolvedPatient.getAttribute(ATTRIBUTE_TELEPHONE), encounterDate);
		

		return resolvedPatient;
	}

	protected Patient initializePatient(Patient hl7Patient, Patient matchedPatient)
	{
		
		Patient resolvedPatient = new Patient();
		resolvedPatient.setPatientId(matchedPatient.getPatientId());
		resolvedPatient.setGender(matchedPatient.getGender());
		resolvedPatient.setAddresses(matchedPatient.getAddresses());
		resolvedPatient.setAttributes(matchedPatient.getAttributes());
		resolvedPatient.setBirthdate(matchedPatient.getBirthdate());
		resolvedPatient.setIdentifiers(matchedPatient.getIdentifiers());
		resolvedPatient.setNames(matchedPatient.getNames());

		// Currently not using these.
		resolvedPatient.setCauseOfDeath(hl7Patient.getCauseOfDeath());
		resolvedPatient.setDead(hl7Patient.getDead());
		resolvedPatient.setDeathDate(hl7Patient.getDeathDate());

		return resolvedPatient;

	}

	/**
	 * Defines the correct address list and returns the best address
	 * 
	 * @param resolvedPatient
	 * @return
	 */
	public PersonAddress getBestAddress(Patient hl7Patient,
			Patient matchedPatient, Date encounterDate)
	{
		PersonAddress hl7Address = hl7Patient.getPersonAddress();
		PersonAddress existingPreferredAddr = matchedPatient.getPersonAddress();
		PersonAddress bestAddress = new PersonAddress();
		bestAddress.setPreferred(true);

		if (hl7Address == null)
		{
			return existingPreferredAddr;
		}

		if (existingPreferredAddr == null)
		{
			hl7Address.setDateCreated(encounterDate);
			matchedPatient.getPersonAddress().setDateCreated(encounterDate);
			return hl7Address;
		}

		String existingPrefAddr1, PIDAddr1;
		String existingPrefAddr2, PIDAddr2;
		String existingPrefCity, PIDCity;
		String existingPrefCountry, PIDCountry;
		String existingPrefPostalCode, PIDPostalCode;
		String existingPrefCounty, PIDCounty;
		String existingState, PIDState;

		existingPrefAddr1 = existingPreferredAddr.getAddress1();
		existingPrefAddr2 = existingPreferredAddr.getAddress2();
		existingPrefCity = existingPreferredAddr.getCityVillage();
		existingState = existingPreferredAddr.getStateProvince();
		existingPrefCountry = existingPreferredAddr.getCountry();
		existingPrefPostalCode = existingPreferredAddr.getPostalCode();
		existingPrefCounty = existingPreferredAddr.getCountyDistrict();

		PIDAddr1 = hl7Address.getAddress1();
		PIDAddr2 = hl7Address.getAddress2();
		PIDCity = hl7Address.getCityVillage();
		PIDState = hl7Address.getStateProvince();
		PIDCountry = hl7Address.getCountry();
		PIDPostalCode = hl7Address.getPostalCode();
		PIDCounty = hl7Address.getCountyDistrict();

		// Check if identical

		if (compare(existingPrefAddr1, PIDAddr1)
				&& compare(existingPrefAddr2, PIDAddr2)
				&& compare(existingPrefCity, PIDCity)
				&& compare(existingPrefCountry, PIDCountry)
				&& compare(existingPrefPostalCode, PIDPostalCode)
				&& compare(existingPrefCounty, PIDCounty)
				&& compare(existingState, PIDState))
		{
			bestAddress = existingPreferredAddr;

		}

		else if (!existingPreferredAddr.getDateCreated().after(
				hl7Address.getDateCreated()))
		{
			// PID is newer or the same date
			if (hl7Address.getAddress1() == null
					|| hl7Address.getAddress1().equals(""))
			{
				hl7Address.setAddress1(existingPrefAddr1);
			}
			if (hl7Address.getAddress2() == null
					|| hl7Address.getAddress2().equals(""))
			{
				hl7Address.setAddress2(existingPrefAddr2);
			}
			if (hl7Address.getCityVillage() == null
					|| hl7Address.getCityVillage().equals(""))
			{
				hl7Address.setCityVillage(existingPrefCity);
			}
			if (hl7Address.getStateProvince() == null
					|| hl7Address.getStateProvince().equals(""))
			{
				hl7Address.setStateProvince(existingState);
			}
			if (hl7Address.getCountry() == null
					|| hl7Address.getCountry().equals(""))
			{
				hl7Address.setCountry(existingPrefCountry);
			}
			if (hl7Address.getPostalCode() == null
					|| hl7Address.getPostalCode().equals(""))
			{
				hl7Address.setPostalCode(existingPrefPostalCode);
			}
			if (hl7Address.getCountyDistrict() == null
					|| hl7Address.getCountyDistrict().equals(""))
			{
				hl7Address.setCountyDistrict(existingPrefCounty);
			}
			bestAddress = hl7Address;
			bestAddress.setDateCreated(encounterDate);

		} else
		{
			// PID is older
			bestAddress = existingPreferredAddr;

		}

		return bestAddress;

	}

	public PatientIdentifier getBestIdentifier(Patient hl7Patient,
			Patient matchedPatient, Date encounterDate)
	{

		PatientIdentifier hl7Identifier = hl7Patient.getPatientIdentifier();
		PatientIdentifier existingPreferredIdent = matchedPatient
				.getPatientIdentifier();
		PatientIdentifier bestIdent = new PatientIdentifier();

		// Check null identifier object
		// If hl7 ident does not exist, no updates. Return the existing
		// preferred identifier
		if (hl7Identifier == null)
		{
			bestIdent = existingPreferredIdent;
			bestIdent.setPreferred(true);
			return bestIdent;
		} else if (existingPreferredIdent == null)
		{
			// if existing identifier object is null, add the new identifier
			bestIdent = hl7Identifier;
			bestIdent.setDateCreated(encounterDate);
			bestIdent.setPreferred(true);
			return bestIdent;
		}

		String hl7Ident = hl7Identifier.getIdentifier();
		if (existingPreferredIdent.getIdentifier().equalsIgnoreCase(hl7Ident))
		{
			// make no changes
			return null;
		}

		// Check for valid hl7 identifier value
		if (hl7Ident == null || hl7Ident.equals(""))
		{
			bestIdent = existingPreferredIdent;
		} else if (!existingPreferredIdent.getDateCreated()
				.after(encounterDate))
		{
			// PID is newer or the same date
			bestIdent = hl7Identifier;
			bestIdent.setDateCreated(encounterDate);

		} else
		{
			// PID is older
			// Add the PID for record, but set preferred status to false.
			bestIdent = existingPreferredIdent;
			bestIdent.setDateCreated(encounterDate);
		}

		bestIdent.setPreferred(true);
		return bestIdent;

	}

	/**
	 * Check names for valid values and update names in database. Check for
	 * nulls, empty strings, and verify most current name.
	 * 
	 * @param hl7Patient - patient from new message
	 * @param matchedPatient - new message demographics were matched to this
	 *        patient
	 * @param encounterDate -
	 * @should return most recent valid name
	 * @should return name with valid format even if valid is older
	 * @should return nonnull fn ln mn even if null is newer
	 * @should return notnull PersonName if one PersonName is null
	 * @return
	 */
	public PersonName getBestName(PersonName hl7Name, PersonName matchName,
			Date encounterDate)
	{

		Date matchNameDate = matchName.getDateCreated();
		PersonName bestName = new PersonName();
		bestName.setCreator(Context.getAuthenticatedUser());
		bestName.setDateCreated(matchNameDate);
		bestName.setPreferred(true);

		// 12/19/2007 - change logic to evaluate each component of the name
		// separately. Don't judge validity based on first name alone.

		// If hl7 name does not exist, no updates, update the date Return the
		// existing name.
		if (hl7Name == null)
		{
			// Make sure that the current name is still set to preferred.
			bestName = matchName;
			bestName.setPreferred(true);
			return matchName;
		}

		// Check for valid names
		String matchLN = matchName.getFamilyName();
		String matchFN = matchName.getGivenName();
		String matchMN = matchName.getMiddleName();
		String hl7LN = hl7Name.getFamilyName();
		String hl7FN = hl7Name.getGivenName();
		String hl7MN = hl7Name.getMiddleName();

		// Identical names, no need to add value.

		if (compare(hl7FN, matchFN) && compare(hl7LN, matchLN) && compare(hl7MN, matchMN))
		{

			return null;
		}

		// FIRST NAME
		else if ((hl7FN == null)
				|| hl7FN.toLowerCase().toLowerCase().equals("baby")
				|| hl7FN.toLowerCase().matches("inf")
				|| hl7FN.toLowerCase().matches("infant")
				|| hl7FN.toLowerCase().matches("infant [a-z0-9]")
				|| hl7FN.toLowerCase().matches("infant girl [a-z0-9]")
				|| hl7FN.toLowerCase().matches("infant girl")
				|| hl7FN.toLowerCase().matches("infant boy")
				|| hl7FN.toLowerCase().matches("infant boy [a-z0-9]")
				|| hl7FN.toLowerCase().matches("inf [a-z0-9]")
				|| hl7FN.toLowerCase().matches("inf boy")
				|| hl7FN.toLowerCase().matches("inf girl")
				|| hl7FN.toLowerCase().matches("inf girl [a-z0-9]")
				|| hl7FN.toLowerCase().matches("inf boy [a-z0-9]")
				|| hl7FN.toLowerCase().matches("boy")
				|| hl7FN.toLowerCase().matches("boy [a-z0-9]")
				|| hl7FN.toLowerCase().matches("girl")
				|| hl7FN.toLowerCase().matches("girl [a-z0-9]")
				|| hl7FN.toLowerCase().matches("baby")
				|| hl7FN.toLowerCase().matches("baby [a-z0-9]")
				|| hl7FN.toLowerCase().matches("baby boy")
				|| hl7FN.toLowerCase().matches("baby boy [a-z0-9]")
				|| hl7FN.toLowerCase().matches("baby girl")
				|| hl7FN.toLowerCase().matches("baby girl [a-z0-9]")
				|| hl7FN.toLowerCase().matches("babygirl")
				|| hl7FN.toLowerCase().matches("babygirl [a-z0-9]")
				|| hl7FN.toLowerCase().matches("babyboy")
				|| hl7FN.toLowerCase().matches("babyboy [a-z0-9]")
				|| hl7FN.toLowerCase().matches(""))

		{
			// PID (hl7) name is not valid
			bestName.setGivenName(matchFN);
			bestName.setDateCreated(matchName.getDateCreated());

		} else
		{
			// PID name is valid; Check date;
			if (!matchName.getDateCreated().after(encounterDate))
			{
				// PID name is newest
				bestName.setGivenName(hl7FN);
				bestName.setDateCreated(encounterDate);
			} else
			{
				// PID is not newest
				bestName.setGivenName(matchFN);
				bestName.setDateCreated(matchName.getDateCreated());
			}

		}
		
		// MIDDLE NAME
		if (hl7MN == null || hl7MN.equals(""))
		{
			// HL7/PID is not valid
			bestName.setMiddleName(matchMN);
			bestName.setDateCreated(matchName.getDateCreated());
		} else
		{
			// PID is valid
			if (!matchName.getDateCreated().after(encounterDate))
			{
				// PID name is newest
				bestName.setMiddleName(hl7MN);
				bestName.setDateCreated(encounterDate);
			} else
			{
				// PID is not newest
				bestName.setMiddleName(matchMN);
				bestName.setDateCreated(matchName.getDateCreated());
			}
		}

		// LAST NAME
		if (hl7LN == null || hl7LN.equals(""))
		{
			// HL7/PID is not valid
			bestName.setFamilyName(matchLN);
			bestName.setDateCreated(matchName.getDateCreated());
		} else
		{
			// PID is valid
			if (!matchName.getDateCreated().after(encounterDate))
			{
				// PID name is newest
				bestName.setFamilyName(hl7LN);
				bestName.setDateCreated(encounterDate);
			} else
			{
				// PID is not newest
				bestName.setFamilyName(matchLN);
				bestName.setDateCreated(matchName.getDateCreated());
			}

		}

		return bestName;

	}

	

	private PersonName parseNKName(PersonAttribute NKNameAttr)
	{
		String firstname = "";
		String lastname = "";
		if (NKNameAttr != null)
		{
			String NKnameValue = NKNameAttr.getValue();
			int index1 = NKnameValue.indexOf("|");
			if (index1 != -1)
			{
				firstname = NKnameValue.substring(0, index1);
				lastname = NKnameValue.substring(index1 + 1);
			} else
			{
				firstname = NKnameValue;
			}

		}

		PersonName NKName = new PersonName();
		NKName.setFamilyName(lastname);
		NKName.setGivenName(firstname);

		return NKName;
	}

	private PersonAttribute getBestNK(Patient hl7Patient,
			Patient matchedPatient, Date encounterDate)
	{

		PersonAttribute matchedNKNameAttr = matchedPatient
				.getAttribute(ATTRIBUTE_NEXT_OF_KIN);
		PersonAttribute hl7NKNameAttr = hl7Patient
				.getAttribute(ATTRIBUTE_NEXT_OF_KIN);
		PersonAttribute bestAttr = new PersonAttribute();
		bestAttr.setVoided(false);
		
		// Parse the name
		if (matchedNKNameAttr == null && hl7NKNameAttr == null)
		{
			return null;
		} else if (matchedNKNameAttr == null)
		{
			return hl7NKNameAttr;
		} else if (hl7NKNameAttr == null)
		{
			return matchedNKNameAttr;
		}

		PersonName hl7PatientNK = parseNKName(hl7NKNameAttr);
		PersonName resolvedPatientNK = parseNKName(matchedNKNameAttr);
		// at this point the resolved name and the matched name are the same.

		String hl7NKln = hl7PatientNK.getFamilyName();
		String hl7NKfn = hl7PatientNK.getGivenName();
		String resNKln = resolvedPatientNK.getFamilyName();
		String resNKfn = resolvedPatientNK.getGivenName();

		// Identical

		if (compare(resNKfn, hl7NKfn) && compare(resNKln, hl7NKln))
		{
			bestAttr = null;

		}
		else if (!matchedNKNameAttr.getDateCreated().after(encounterDate))
		{
			// hl7 message date is more recent

			bestAttr = hl7NKNameAttr;
			bestAttr.setDateCreated(encounterDate);
			bestAttr.setCreator(Context.getAuthenticatedUser());
		} else
		{
			// existing non voided name is the best name
			bestAttr = matchedNKNameAttr;

		}

		return bestAttr;

	}

	
	/**
	 * @param hl7
	 * @param resolved
	 * @param encounterDate
	 * @should return non null attribute 
	 * @should return newer attribute
	 * @return
	 */
	public PersonAttribute getBestTel(PersonAttribute hl7,
			PersonAttribute resolved, Date encounterDate)
	{
		
		PersonAttribute bestTel = null;
		if (hl7 == null){
			bestTel = resolved;
			if (bestTel != null){
				bestTel.setVoided(false);
			}
			return bestTel;
		}
		if (resolved == null){
			bestTel = hl7;
			bestTel.setVoided(false);
			bestTel.setDateCreated(encounterDate);
			bestTel.setCreator(Context.getAuthenticatedUser());
			return bestTel;
		}
		
		String PIDTel = hl7.getValue();
		String resolvedTel = resolved.getValue();
		if (resolvedTel == null || resolvedTel.equals("")){
			bestTel = hl7;
			bestTel.setDateCreated(encounterDate);
			bestTel.setCreator(Context.getAuthenticatedUser());
		}
		
		if  (resolvedTel.equals(PIDTel))
		{
			bestTel = hl7;
			bestTel.setDateCreated(encounterDate);
			bestTel.setCreator(Context.getAuthenticatedUser());
		}
		if (PIDTel == null || PIDTel.equals("")){
			bestTel = resolved;
		}
		
		else if (!resolved.getDateCreated().after(encounterDate))
		{
			// PID message date is more recent
			bestTel = hl7;
			bestTel.setDateCreated(encounterDate);
			bestTel.setCreator(Context.getAuthenticatedUser());
			
		} else
		{
			// do not add hl7 attribute
			bestTel = resolved;
		}

		bestTel.setVoided(false);
		return bestTel;
	}


	private String getBestGender(Patient hl7Patient, Patient resolvedPatient)
	{

		String matchGender = resolvedPatient.getGender();
		String hl7Gender = hl7Patient.getGender();
		String resolvedGender = "";

		if (hl7Gender != null && !hl7Gender.equals(""))
		{
			resolvedGender = hl7Gender;
		} else
		{
			resolvedGender = matchGender;
		}

		return resolvedGender;

	}

	private Date getBestDOB(Patient hl7Patient, Patient resolvedPatient)
	{

		Date matchDOB = resolvedPatient.getBirthdate();
		Date hl7DOB = hl7Patient.getBirthdate();
		Date resolvedDOB = hl7DOB;

		if (hl7DOB != null)
		{
			resolvedDOB = hl7DOB;
		} else
		{
			resolvedDOB = matchDOB;
		}

		return resolvedDOB;

	}

	

	public boolean compare(String str1, String str2)
	{
		boolean match = false;
		match = (str1 == null && str2 == null)
				|| ((str1 != null && str2 != null) && str1
						.equalsIgnoreCase(str2.trim()));

		return match;
	}

	
	private static PersonAttributeType createAttributeType(String patString)
	{
		
		PersonAttributeType personAttr = new PersonAttributeType();
		try
		{
			personAttr.setName(patString);
			personAttr.setFormat("java.lang.String");
			personAttr.setDescription(patString);
			personAttr.setSearchable(true);
			PersonService personService = Context.getPersonService();
			personService.savePersonAttributeType(personAttr);
			Context.clearSession();
			Context.closeSession();
		} catch (RuntimeException e)
		{
			logger.error("Unable to create new attribute type:" + patString);
			logger.error(e.getStackTrace());
		}
		return personAttr;

	}
}
