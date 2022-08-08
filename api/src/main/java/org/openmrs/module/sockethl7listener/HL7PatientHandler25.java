/**
 * 
 */
package org.openmrs.module.sockethl7listener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonAddress;
import org.openmrs.PersonName;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.chirdlutil.util.ChirdlUtilConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.datatype.CE;
import ca.uhn.hl7v2.model.v25.datatype.CX;
import ca.uhn.hl7v2.model.v25.datatype.FN;
import ca.uhn.hl7v2.model.v25.datatype.IS;
import ca.uhn.hl7v2.model.v25.datatype.SAD;
import ca.uhn.hl7v2.model.v25.datatype.ST;
import ca.uhn.hl7v2.model.v25.datatype.TS;
import ca.uhn.hl7v2.model.v25.datatype.XAD;
import ca.uhn.hl7v2.model.v25.datatype.XPN;
import ca.uhn.hl7v2.model.v25.datatype.XTN;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.segment.NK1;
import ca.uhn.hl7v2.model.v25.segment.PID;

/**
 * @author tmdugan
 * 
 */
public class HL7PatientHandler25 implements HL7PatientHandler
{

	private static final String MRN_PREFIX = "MRN_";
	private static final String GENERIC_ASSIGNING_AUTHORITY = "OTHER";
	private static final Logger log =  LoggerFactory.getLogger("SocketHandlerLogger");

	protected PID getPID(Message message)
	{
		if (message instanceof ORU_R01)
		{
			return getPID((ORU_R01) message);
		}
		if (message instanceof ADT_A01)
		{
			return getPID((ADT_A01) message);
		}
		return null;
	}

	private PID getPID(ADT_A01 message)
	{
		return message.getPID();
	}

	private PID getPID(ORU_R01 oru)
	{
		return oru.getPATIENT_RESULT().getPATIENT().getPID();
	}
	
	protected NK1 getNK1(Message message)
	{
		if (message instanceof ORU_R01)
		{
			return getNK1((ORU_R01) message);
		}
		if (message instanceof ADT_A01)
		{
			return getNK1((ADT_A01) message);
		}
		return null;
	}

	private NK1 getNK1(ADT_A01 adt)
	{
		return adt.getNK1();
	}

	private NK1 getNK1(ORU_R01 oru)
	{
		return oru.getPATIENT_RESULT().getPATIENT().getNK1();
	}

	protected Date TranslateDate(TS ts)
	{
		return HL7ObsHandler25.TranslateDate(ts);
	}

	protected PersonAddress getAddress(XAD xad)
	{
		PersonAddress address = new PersonAddress();
		SAD streetAddress;
		String city = "";
		String stateProvince = "";
		String country = "";
		String postalCode = "";
		String streetAddrString = "";
		String otherDesignation = "";

		if (xad != null)
		{
			streetAddress = xad.getStreetAddress();
			streetAddrString = streetAddress.getStreetOrMailingAddress()
					.toString();
			otherDesignation = xad.getOtherDesignation().toString();
			city = xad.getCity().toString();

			stateProvince = xad.getStateOrProvince().toString();
			postalCode = xad.getZipOrPostalCode().toString();
			country = xad.getCountry().toString();

		}

		address.setAddress1(streetAddrString);
		address.setAddress2(otherDesignation);
		address.setCityVillage(city);
		address.setStateProvince(stateProvince);
		address.setCountry(country);
		address.setPostalCode(postalCode);
		UUID uuid = UUID.randomUUID();
		address.setUuid(uuid.toString()); 
		address.setPreferred(true);

		return address;
	}
	
	// This provides a place holder for parsed hl7
	// elements that are not being stored
	private void unUsedValues(Message message)
	{
		PID pid = getPID(message);

		// Alternate Patient ID
		// Alternate ID not currently used in patient table
		CX[] alternateID = null;
		alternateID = pid.getAlternatePatientIDPID();
		String altID = "";
		for (CX alt : alternateID)
		{
			altID = alt.getIDNumber().toString();
		}
		// County Code
		// As of yet, db does not contain a field to store this value
		// TODO County Code: determine field in db and set accordingly
		IS countyCode = null;
		String cntyCode = "";
		countyCode = pid.getCountyCode();
		cntyCode = countyCode.getValue();

		// Phone Number --Home
		// As of yet, db does not contain a field to store this value
		// TODO Phone Number Home: determine field in db and set accordingly
		XTN[] homeNumber = null;
		homeNumber = pid.getPhoneNumberHome();
		String strHomeNumber = "";
		for (XTN num : homeNumber)
		{
			strHomeNumber = num.getTelephoneNumber().toString();
		}
		// SSN
		// As of yet, db does not contain a field to store this value
		// TODO SSN: determine field in db and set accordingly
		String SSN = "";
		SSN = pid.getSSNNumberPatient().toString();

		// Ethnic Group
		// As of yet, openmrs db does store this value
		// TODO Ethnic Group: determine field in db and set accordingly with api
		CE[] ceEGroup = null;
		ceEGroup = pid.getEthnicGroup();
		String eGroup = "";
		for (CE egroup : ceEGroup)
		{
			eGroup = egroup.getIdentifier().toString();
		}
	}

	protected String getMRN(CX ident)
	{
		String stIdent = null;

		ST id;
		if ((id = ident.getIDNumber()) != null)
		{
			stIdent = id.getValue();
		}
		return stIdent;
	}

	public PersonName getPatientName(Message message)
	{
		PersonName name = new PersonName();
		PID pid = getPID(message);
		String ln = "", fn = "", mn = "";
		XPN[] xpnList = null;
		try
		{
			xpnList = pid.getPatientName();
			XPN xpn = xpnList[0];
			FN FNln = xpn.getFamilyName();
			ST STfn = xpn.getGivenName();
			ST STmn = xpn.getSecondAndFurtherGivenNamesOrInitialsThereof();

			if (FNln != null && FNln.getSurname() != null)
			{
				String lnvalue = org.openmrs.module.chirdlutil.util.Util
						.toProperCase(FNln.getSurname().getValue());
				if (lnvalue != null)
					ln = lnvalue;
			}

			if (STfn != null)
			{
				String fnvalue = org.openmrs.module.chirdlutil.util.Util.toProperCase(STfn.getValue());
				if (fnvalue != null)
					fn = fnvalue;
			}

			if (STmn != null)
			{
				String mnvalue = org.openmrs.module.chirdlutil.util.Util.toProperCase(STmn.getValue());
				if (mnvalue != null)
					mn = mnvalue;
			}

			name.setFamilyName(ln.replaceAll("\"", ""));
			name.setGivenName(fn.replaceAll("\"", ""));
			name.setMiddleName(mn.replaceAll("\"", ""));
			UUID uuid = UUID.randomUUID();
			name.setUuid(uuid.toString()); 
			// set preferred to true because this method
			// deliberately just processes the first person name
			name.setPreferred(true);

		} catch (RuntimeException e)
		{
			log.error("Unable to parse patient name from PID.", e);
		}

		return name;
	}

	public String getRace(Message message)
	{
		CE[] ceRace = null;
		PID pid = getPID(message);
		try
		{
			ceRace = pid.getRace();
		} catch (RuntimeException e)
		{
			log.error("Unable to parse race from PID.",e);
		}

		if (ceRace != null)
		{
			try
			{
				return ceRace[0].getIdentifier().toString();
			} catch (RuntimeException e)
			{
				log.error("Exception getting race identifier from PID.", e);
			}
		}
		return null;
	}

	public Date getBirthdate(Message message)
	{
		PID pid = getPID(message);
		TS DOB = pid.getDateTimeOfBirth();
		return TranslateDate(DOB);
	}

	public List<PersonAddress> getAddresses(Message message)
	{
		// ***For Newborn screening we are using the next-of-kin address
		// and not the patient address from PID
		PID pid = getPID(message);
		NK1 nk1 = getNK1(message);
		List<PersonAddress> addresses = new ArrayList<>();
		try
		{
			XAD[] xadAddresses = pid.getPatientAddress(); // PID address
			if (xadAddresses.length == 0)
			{
				xadAddresses = nk1.getAddress();
			}
			if (xadAddresses.length == 0)
			{
				XAD xadAddress = null;
				addresses.add(getAddress(xadAddress));
			} else
			{
				for (XAD xadAddress : xadAddresses)
				{
					addresses.add(getAddress(xadAddress));
				}
			}

		} catch (RuntimeException e)
		{
			log.error("Unable to parse address from PID", e);
		}
		return addresses;
	}

	public String getTelephoneNumber(Message message)
	{
		PID pid = getPID(message);
		NK1 nk1 = getNK1(message);
		String tNumber = "";
		XTN[] telnumbers = pid.getPhoneNumberHome();

		if (telnumbers.length == 0)
		{
			telnumbers = nk1.getPhoneNumber();
		}

		if (telnumbers.length > 0)
		{
			tNumber = telnumbers[0].getTelephoneNumber().toString();
		}
		return tNumber;
	}

	public String getBirthplace(Message message)
	{
		PID pid = getPID(message);
		String birthPlace = "";
		try
		{
			birthPlace = pid.getBirthPlace().toString();
			if (birthPlace == null)
			{
				birthPlace = " ";
			}
		} catch (RuntimeException e)
		{
			log.error("Unable to parse birthplace from PID.", e);
		}
		return birthPlace;
	}

	public String getGender(Message message)
	{
		// Gender -- Based on meeting 04/10/2007
		// HL7 for newborn screening will contain F,M, or U, with no preceding
		// codes
		String g = "";
		PID pid = getPID(message);
		try
		{
			g = pid.getAdministrativeSex().getValue();
			if (g == null
					|| ! (g.toLowerCase().equals("m")
							|| g.toLowerCase().equals("f") || g.toLowerCase()
							.equals("u")))
			{
				g = "";
			}
		} catch (RuntimeException e)
		{
			log.error("Unable to parse gender from PID.", e);
		}
		return g;
	}

	public Date getDeathDate(Message message)
	{
		PID pid = getPID(message);
		TS DDT = pid.getPatientDeathDateAndTime();
		Date ddt = TranslateDate(DDT);
		if (DDT.getTime().getValue() == null)
		{
			ddt = null;
		}
		return ddt;
	}

	public Boolean isDead(Message message)
	{
		PID pid = getPID(message);
		boolean isDead = false;
		if (pid.getPatientDeathIndicator().getValue() != null)
		{
			isDead = pid.getPatientDeathIndicator().getValue().equals("1");
		}
		return isDead;
	}

	public Date getDateChanged(Message message)
	{
		PID pid = getPID(message);
		TS tsLastUpdate = pid.getLastUpdateDateTime();
		return TranslateDate(tsLastUpdate);
	}

	/**
	 * This method will parse next of kin from the NK1 segment
	 * 
	 * @param message
	 * @return String with a value of "firstName|lastName" or empty if both first and last name fields were empty
	 * 
	 */
	public String getNextOfKin(Message message){
		String nextOfKin = ChirdlUtilConstants.GENERAL_INFO_EMPTY_STRING;	
		NK1 nk1 = getNK1(message);	

		try
		{
			XPN[] list = null;
			list = nk1.getNKName();
			String nkln = ChirdlUtilConstants.GENERAL_INFO_EMPTY_STRING, nkfn = ChirdlUtilConstants.GENERAL_INFO_EMPTY_STRING;

			if (list != null)
			{
				for (XPN identifier : list)
				{
					nkln = org.openmrs.module.chirdlutil.util.Util.toProperCase(identifier.getFamilyName()
							.getSurname().getValue());
					nkfn = org.openmrs.module.chirdlutil.util.Util.toProperCase(identifier.getGivenName()
							.getValue());
				}
			}

			if (nkln == null)
				nkln = ChirdlUtilConstants.GENERAL_INFO_EMPTY_STRING;
			if (nkfn == null)
				nkfn = ChirdlUtilConstants.GENERAL_INFO_EMPTY_STRING;
			
			if(StringUtils.isNotBlank(nkfn) || StringUtils.isNotBlank(nkln)) 	
			{
				// CHICA-1185 Only add the pipe delimiter if there is a value in at least the first OR last name field
				// If both fields are empty, this method will return an empty string
				nextOfKin = nkfn + ChirdlUtilConstants.GENERAL_INFO_PIPE_DELIMITER + nkln;
			}
		} catch (RuntimeException e)
		{
			log.error("Exception parsing next-of-kin from PID", e);
		}
		return nextOfKin;
	}
	

	/* (non-Javadoc)
	 * @see org.openmrs.module.sockethl7listener.HL7PatientHandler#getMothersName(ca.uhn.hl7v2.model.Message)
	 */
	public String getMothersName(Message message) {
		
		NK1 nk1 = getNK1(message);
		String motherNameString = "";

		try {

			XPN[] mnList = null;

			mnList = nk1.getNKName();

			String nkln = "";
			String nkfn = "";

			if (mnList != null) {
				for (XPN identifier : mnList) {
					nkln = org.openmrs.module.chirdlutil.util.Util
							.toProperCase(identifier.getFamilyName().getSurname().getValue());
					nkfn = org.openmrs.module.chirdlutil.util.Util.toProperCase(identifier.getGivenName().getValue());

				}
			}

			if (nkln == null)
				nkln = "";
			if (nkfn == null)
				nkfn = "";
			motherNameString = nkfn + "|" + nkln;
		} catch (RuntimeException e) {
			log.error("Exception parsing next-of-kin from PID.", e);
		}
		return motherNameString;
	}

	public String getCitizenship(Message message)
	{
		PID pid = getPID(message);
		CE[] ceCitizen = null;
		try 
		{
			ceCitizen = pid.getCitizenship();
		} catch (RuntimeException e)
		{
			log.error("Unable to parse citizenship field from PID.", e);
		}
		
		if (ceCitizen != null) {
			try
			{
				return ceCitizen[0].getIdentifier().toString();
			} catch (RuntimeException e)
			{
				log.error("Exception parsing citizenship identifier from PID citizenship field.", e);
			}
		}
		
		return null;
	}

	public Set<PatientIdentifier> getIdentifiers(Message message) {
		PID pid = getPID(message);
		CX[] identList = null;
		PatientService patientService = Context.getPatientService();
		Set<PatientIdentifier> identifiers = new TreeSet<>();

		try {

			identList = pid.getPatientIdentifierList();
		} catch (RuntimeException e) {
			// Exception in Hapi method for parsing identifiers from PID segment
			// Execute find match without the identifier. Some applications do not need MRN
			// for lookup.
			log.error("Exception parsing patient identifiers from PID segment. ", e);
			return identifiers;
		}
		if (identList == null || identList.length == 0) {
			// Some applications do not need MRN for lookup. Execute find match without the
			// identifier
			log.error("No patient identifiers in PID.");
			return identifiers;
		}

		// MES - CHICA-438 - When there are > 1 identifiers, set only the first to
		// preferred.
		boolean preferred = true;
		for (CX ident : identList) {
			// First set up the identifier type; We currently use MRN
			// Get the id number for the authorizing facility

			PatientIdentifierType pit = new PatientIdentifierType();
			PatientIdentifier pi = new PatientIdentifier();
			String stIdent = "";
			String assignAuth = "";

			assignAuth = ident.getAssigningAuthority().getNamespaceID().getValue();

			if ((pit = patientService.getPatientIdentifierTypeByName(MRN_PREFIX + assignAuth)) == null) {
				pit = patientService.getPatientIdentifierTypeByName(MRN_PREFIX + GENERIC_ASSIGNING_AUTHORITY);
			}

			// DWE CHICA-771 Store MRN with leading zeros as MRN_EHR identifier
			if (pit.getName().equals(ChirdlUtilConstants.IDENTIFIER_TYPE_MRN_EHR)) {
				stIdent = getIDNumberValue(ident);
			} else {
				// Existing functionality always removes leading zeros and adds "-" before the
				// check digit
				stIdent = getMRN(ident);
			}

			if (stIdent != null && stIdent.length() > 0) {
				pi.setIdentifierType(pit);
				pi.setIdentifier(stIdent);
				pi.setPreferred(preferred);
				identifiers.add(pi);
				preferred = false;

			} else {
				log.error("Patient identifier name in CX field is empty for identifier type: {} patient:{} ", pit.getName(), pid.getPatientID());
			}

		}

		return identifiers;
	}

	public String[] getPatientIdentifierList(Message message)
	{
		String[] patientIdentsAsString = null;
		CX[] pIdentifierList = getPID(message).getPatientIdentifierList();
		if (pIdentifierList != null)
		{
			patientIdentsAsString = new String[pIdentifierList.length];

			for (int i = 0; i < pIdentifierList.length; i++)
			{
				CX patId = pIdentifierList[i];
				patientIdentsAsString[i] = getMRN(patId);
			}
		}
		return patientIdentsAsString;
	}
	
	/**
	 * DWE CHICA-406
	 */
	public String getAccountNumber(Message message)
	{
		CX accountNumber = null;
		PID pid = getPID(message);
		try
		{
			accountNumber = pid.getPatientAccountNumber();
		} catch (RuntimeException e)
		{
			log.error("Unable to parse patient account number from PID.", e);
		}

		if (accountNumber != null)
		{
			try
			{
				return accountNumber.getIDNumber().toString();
			} catch (RuntimeException e)
			{
				log.error("Exception getting Patient account number from PID CX field.", e);
			}
		}
		return null;
	}
	
	/**
	 * DWE CHICA-702
	 * 
	 * HL7 Version 2.5 Parse ethnicity code from PID-22
	 * 
	 * @param message
	 * @return ethnicity code
	 */
	public String getEthnicity(Message message)
	{
		CE[] ceEthnicGroup = null;
		PID pid = getPID(message);
		try
		{
			ceEthnicGroup = pid.getEthnicGroup();
		} 
		catch (RuntimeException e)
		{
			log.error("Unable to parse ethnic group from PID", e);
		}

		if (ceEthnicGroup != null)
		{
			try
			{
				return ceEthnicGroup[0].getIdentifier().toString();
			} 
			catch (RuntimeException e1)
			{
				log.error("Unable to parse ethnic group identifier in PID.");
			}
		}
		return null;
	}
	
	/**
	 * DWE CHICA-771 
	 * Get the string value of the identifier
	 * @param ident
	 * @return
	 */
	public String getIDNumberValue(CX ident)
	{
		String identifier = null;
		ST id = ident.getIDNumber();
		if (id != null)
		{
			identifier = id.getValue();
		}
		return identifier;
	}
}
