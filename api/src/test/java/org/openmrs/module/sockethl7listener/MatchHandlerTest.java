package org.openmrs.module.sockethl7listener;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;

public class MatchHandlerTest extends BaseModuleContextSensitiveTest {
	/**
	 * @see {@link MatchHandler#getBestName(Patient,Patient,Date)}
	 * 
	 */
	protected static final String DATASET_XML = "dbunit/BasicTest.xml";

	@BeforeEach
	public void runBeforeEachTest() throws Exception {
		initializeInMemoryDatabase();
		executeDataSet(DATASET_XML);
		authenticate();
	}

	@Test
	public void getBestName_shouldReturnMostRecentValidName() throws Exception {
		int TEST_PATIENT = 33;
		int TEST_PATIENT_MORE_RECENT = 35;

		PatientService patientService = Context.getPatientService();
		Patient matchPatient = patientService.getPatient(TEST_PATIENT);
		Patient hl7Patient = patientService.getPatient(TEST_PATIENT_MORE_RECENT);
		MatchHandler mh = new MatchHandler();
		Assertions.assertNotNull(hl7Patient.getPersonName());
		Assertions.assertNotNull(matchPatient.getPersonName());
		PersonName resultName = mh.getBestName(hl7Patient.getPersonName(), matchPatient.getPersonName(), new Date());
		Assertions.assertNotNull(resultName);
		Assertions.assertEquals("Jenny", resultName.getGivenName());
		Assertions.assertEquals("", resultName.getMiddleName());
		Assertions.assertEquals("Patient", resultName.getFamilyName());

		return;
	}

	/**
	 * 
	 * @see {@link MatchHandler#getBestName(PersonName,PersonName,Date)}
	 * 
	 */
	@Test
	public void getBestName_shouldReturnNameWithValidFormatEvenIfValidIsOlder() throws Exception {

		// Name with invalid first name is more recent. Return the latest known valid
		// first name

		PersonName nameNew = new PersonName("infant", "Joseph", "Smith");
		Calendar cal1 = Calendar.getInstance();
		// Clear all fields
		cal1.clear();
		cal1.set(Calendar.YEAR, 2008);
		cal1.set(Calendar.MONTH, 3);
		cal1.set(Calendar.DATE, 4);
		Date encounterdate = cal1.getTime();

		PersonName nameExists = new PersonName("John", "David", "Jones");
		Calendar cal2 = Calendar.getInstance();
		// Clear all fields
		cal2.clear();
		cal2.set(Calendar.YEAR, 2008);
		cal2.set(Calendar.MONTH, 3);
		cal2.set(Calendar.DATE, 3);
		Date date2 = cal2.getTime();
		nameExists.setDateCreated(date2);
		MatchHandler mh = new MatchHandler();
		PersonName resultName = mh.getBestName(nameNew, nameExists, encounterdate);
		Assertions.assertNotNull(resultName);
		Assertions.assertEquals("John", resultName.getGivenName());
		Assertions.assertEquals("Joseph", resultName.getMiddleName());
		Assertions.assertEquals("Smith", resultName.getFamilyName());

		return;
	}

	/**
	 * @see {@link MatchHandler#getBestName(PersonName,PersonName,Date)}
	 * 
	 */
	@Test
	public void getBestName_shouldReturnNonnullFnLnMnEvenIfNullIsNewer() throws Exception {

		PersonName nameNew = new PersonName(null, "Jack", "Smith");
		Calendar cal1 = Calendar.getInstance();
		// Clear all fields
		cal1.clear();
		cal1.set(Calendar.YEAR, 2008);
		cal1.set(Calendar.MONTH, 3);
		cal1.set(Calendar.DATE, 4);
		Date encounterdate = cal1.getTime();

		PersonName nameExists = new PersonName("John", "James", "Jones");
		Calendar cal2 = Calendar.getInstance();
		// Clear all fields
		cal2.clear();
		cal2.set(Calendar.YEAR, 2008);
		cal2.set(Calendar.MONTH, 3);
		cal2.set(Calendar.DATE, 3);
		Date date2 = cal2.getTime();
		nameExists.setDateCreated(date2);
		MatchHandler mh = new MatchHandler();
		PersonName resultName = mh.getBestName(nameNew, nameExists, encounterdate);
		Assertions.assertNotNull(resultName);
		Assertions.assertEquals("John", resultName.getGivenName());
		Assertions.assertEquals("Jack", resultName.getMiddleName());
		Assertions.assertEquals("Smith", resultName.getFamilyName());
		Assertions.assertTrue(resultName.getPreferred());
		Assertions.assertTrue(resultName.getDateCreated().equals(encounterdate));

	}

	/**
	 * @see {@link MatchHandler#getBestName(PersonName,PersonName,Date)}
	 * 
	 */
	@Test

	public void getBestName_shouldReturnNotnullPersonNameIfOnePersonNameIsNull() throws Exception {

		PersonName nameNew = null;
		Calendar cal1 = Calendar.getInstance();
		// Clear all fields
		cal1.clear();
		cal1.set(Calendar.YEAR, 2008);
		cal1.set(Calendar.MONTH, 3);
		cal1.set(Calendar.DATE, 4);
		Date encounterdate = cal1.getTime();

		PersonName nameExists = new PersonName("John", "James", "Jones");
		Calendar cal2 = Calendar.getInstance();
		cal2.clear();
		cal2.set(Calendar.YEAR, 2008);
		cal2.set(Calendar.MONTH, 3);
		cal2.set(Calendar.DATE, 3);
		Date date2 = cal2.getTime();
		nameExists.setDateCreated(date2);
		MatchHandler mh = new MatchHandler();
		PersonName resultName = mh.getBestName(nameNew, nameExists, encounterdate);
		Assertions.assertNotNull(resultName);
		Assertions.assertEquals("John", resultName.getGivenName());
		Assertions.assertEquals("James", resultName.getMiddleName());
		Assertions.assertEquals("Jones", resultName.getFamilyName());
		Assertions.assertTrue(resultName.getPreferred());
		Assertions.assertTrue(date2.equals(resultName.getDateCreated()));

	}

	/**
	 * @see {@link MatchHandler#getBestTel(PersonAttribute,PersonAttribute,Date)}
	 * 
	 */
	@Test
	public void getBestTel_shouldReturnNewerAttribute() throws Exception {

		PersonAttributeType pat = new PersonAttributeType();
		pat.setName("Telephone Number");

		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(Calendar.YEAR, 2008);
		cal.set(Calendar.MONTH, 3);
		cal.set(Calendar.DATE, 3);
		Date date = cal.getTime();
		PersonAttribute newAttr = new PersonAttribute(pat, "1234567890");
		newAttr.setDateCreated(date);
		newAttr.setVoided(false);

		Calendar cal2 = Calendar.getInstance();
		cal2.clear();
		cal2.set(Calendar.YEAR, 2007);
		cal2.set(Calendar.MONTH, 4);
		cal2.set(Calendar.DATE, 2);
		Date date2 = cal2.getTime();
		PersonAttribute oldAttr = new PersonAttribute(pat, "222222222");
		oldAttr.setDateCreated(date2);
		oldAttr.setVoided(false);

		MatchHandler mh = new MatchHandler();
		PersonAttribute result = mh.getBestTel(newAttr, oldAttr, date);
		Assertions.assertEquals("1234567890", result.getValue());
		Assertions.assertTrue(result.getDateCreated().equals(date));

	}

	/**
	 * @see {@link MatchHandler#getBestTel(PersonAttribute,PersonAttribute,Date)}
	 * 
	 */
	@Test
	public void getBestTel_shouldReturnNonNullAttribute() throws Exception {

		PersonAttributeType pat = new PersonAttributeType();
		pat.setName("Telephone Number");

		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(Calendar.YEAR, 2008);
		cal.set(Calendar.MONTH, 3);
		cal.set(Calendar.DATE, 3);
		Date date = cal.getTime();
		PersonAttribute newAttr = null;

		Calendar cal2 = Calendar.getInstance();
		cal2.clear();
		cal2.set(Calendar.YEAR, 2007);
		cal2.set(Calendar.MONTH, 4);
		cal2.set(Calendar.DATE, 2);
		Date date2 = cal2.getTime();
		PersonAttribute oldAttr = new PersonAttribute(pat, "222222222");
		oldAttr.setDateCreated(date2);
		oldAttr.setVoided(false);

		MatchHandler mh = new MatchHandler();
		PersonAttribute result = mh.getBestTel(newAttr, oldAttr, date);
		Assertions.assertEquals("222222222", result.getValue());
		Assertions.assertTrue(result.getDateCreated().equals(date2));

	}
	
	@Test
	public void testtGetBestAddress(){
		    
			Calendar PID = Calendar.getInstance();
			Calendar match = Calendar.getInstance();
		
			//PID newer
			Patient p = new Patient();
			PersonAddress pAddr = new PersonAddress();
			PID.set(2007, Calendar.JULY, 10);
			pAddr.setDateCreated(PID.getTime()); 
			pAddr.setAddress1("55 Green St");
			pAddr.setAddress2("");
			pAddr.setCityVillage("Florida");
			pAddr.setCountry("US");
			pAddr.setCountyDistrict("Marion");
			pAddr.setPostalCode("44444");
			p.addAddress(pAddr);
			
			Patient matchedP = new Patient();
			PersonAddress matchedPAddr = new PersonAddress();
			match.set(2007, Calendar.JUNE, 10);
			matchedPAddr.setDateCreated(match.getTime()); 
			matchedPAddr.setAddress1("22 Red St");
			matchedPAddr.setAddress2("");
			matchedPAddr.setCityVillage("Indy");
			matchedPAddr.setCountry("US");
			matchedPAddr.setCountyDistrict("Marion");
			matchedPAddr.setPostalCode("55555");
			matchedP.addAddress(matchedPAddr);
		
			
			MatchHandler mh = new MatchHandler();	
			PersonAddress bestAddr = mh.getBestAddress(p, matchedP, PID.getTime());

			Assertions.assertEquals("55 Green St", bestAddr.getAddress1());
			Assertions.assertEquals("", bestAddr.getAddress2());
			Assertions.assertEquals("Florida", bestAddr.getCityVillage());
			Assertions.assertEquals("US", bestAddr.getCountry());
			Assertions.assertEquals("Marion", bestAddr.getCountyDistrict());
			Assertions.assertEquals("44444", bestAddr.getPostalCode());
			Assertions.assertEquals(pAddr.getDateCreated(),bestAddr.getDateCreated());
			
//			PID older
			Patient p2 = new Patient();
			PersonAddress pAddr2 = new PersonAddress();
			PID.set(2007, Calendar.JULY, 10);
			pAddr2.setDateCreated(PID.getTime()); 
			pAddr2.setAddress1("55 Green St");
			pAddr2.setAddress2("");
			pAddr2.setCityVillage("Florida");
			pAddr2.setCountry("US");
			pAddr2.setCountyDistrict("Marion");
			pAddr2.setPostalCode("44444");
			p2.addAddress(pAddr2);
			
			Patient matchedP2 = new Patient();
			PersonAddress matchedPAddr2 = new PersonAddress();
			match.set(2007, Calendar.AUGUST, 10);
			matchedPAddr2.setDateCreated(match.getTime()); 
			matchedPAddr2.setAddress1("22 August St");
			matchedPAddr2.setAddress2("");
			matchedPAddr2.setCityVillage("Detroit");
			matchedPAddr2.setCountry("US");
			matchedPAddr2.setCountyDistrict("");
			matchedPAddr2.setPostalCode("11111");
			matchedP2.addAddress(matchedPAddr2);
		
			
			MatchHandler mh2 = new MatchHandler();
			
			PersonAddress bestAddr2 = mh2.getBestAddress(p2, matchedP2, PID.getTime());
			
			Assertions.assertEquals("22 August St", bestAddr2.getAddress1());
			Assertions.assertEquals("", bestAddr2.getAddress2());
			Assertions.assertEquals("Detroit", bestAddr2.getCityVillage());
			Assertions.assertEquals("US", bestAddr2.getCountry());
			Assertions.assertEquals("", bestAddr2.getCountyDistrict());
			Assertions.assertEquals("11111", bestAddr2.getPostalCode());
			Assertions.assertEquals(matchedPAddr2.getDateCreated(),bestAddr2.getDateCreated());
			
		}
	
	@Test
	public void testGetBestIdentifierWithMoreRecentHL7Patient(){
		Calendar Hl7Date = Calendar.getInstance();
		Calendar matchDate = Calendar.getInstance();
	
		//Hl7 is newer than existing patient
		Patient hl7Patient = new Patient();
		PatientIdentifier HL7Identifier = new PatientIdentifier();
		
		Hl7Date.set(2007, Calendar.JULY, 10);
		HL7Identifier.setDateCreated(Hl7Date.getTime());
		HL7Identifier.setIdentifier("1234");
		hl7Patient.addIdentifier(HL7Identifier);
		
		Patient matchedPatient = new Patient();
		PatientIdentifier matchedIdentifier = new PatientIdentifier();
		
		matchDate.set(2007, Calendar.JUNE, 1);
		matchedIdentifier.setDateCreated(matchDate.getTime());
		matchedIdentifier.setIdentifier("4321");
		matchedPatient.addIdentifier(matchedIdentifier);
		 
		MatchHandler matchHandler = new MatchHandler();
		PatientIdentifier bestIdentifier = matchHandler.getBestIdentifier(hl7Patient, matchedPatient, Hl7Date.getTime());
	
		Assertions.assertEquals("1234",bestIdentifier.getIdentifier());
		Assertions.assertTrue(bestIdentifier.getPreferred());
		Assertions.assertEquals(HL7Identifier.getDateCreated(),bestIdentifier.getDateCreated());
		
	}
	
	@Test
	public void testGetBestIdentifierWithHL7DateOlder(){
		//HL7 message is older than existing patient
		Calendar Hl7Date = Calendar.getInstance();
		Calendar matchDate = Calendar.getInstance();
		
		Patient hl7Patient = new Patient();
		PatientIdentifier Hl7PatientIdentifier = new PatientIdentifier();
		Hl7Date.set(2007, Calendar.APRIL, 10);
		Hl7PatientIdentifier.setDateCreated(Hl7Date.getTime());
		Hl7PatientIdentifier.setIdentifier("1234");
		hl7Patient.addIdentifier(Hl7PatientIdentifier);   
		
		Patient matchedPatient = new Patient();
		PatientIdentifier matchedPatientIdentifier = new PatientIdentifier();
		
		matchDate.set(2007, Calendar.JUNE, 1);
		matchedPatientIdentifier.setDateCreated(matchDate.getTime());
		matchedPatientIdentifier.setIdentifier("4321");
		matchedPatient.addIdentifier(matchedPatientIdentifier);
		
		MatchHandler matchHandler = new MatchHandler(); 
	    PatientIdentifier bestIdentifier = matchHandler.getBestIdentifier(hl7Patient, matchedPatient,Hl7Date.getTime());
	    
	    Assertions.assertEquals("4321",bestIdentifier.getIdentifier());
	    Assertions.assertEquals("true", bestIdentifier.getPreferred().toString());
	    Assertions.assertEquals(matchedPatientIdentifier.getDateCreated(),bestIdentifier.getDateCreated());
	}
	
	
	
}
