package org.openmrs.module.sockethl7listener;


import static org.junit.Assert.assertFalse;

import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonName;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;

@SkipBaseSetup
public class HL7SocketHandlerTest extends BaseModuleContextSensitiveTest{
	
	protected static final String DATASET_XML = "test/dbunit/BasicTest.xml";
	protected static PatientService patientService = null;
	protected static AdministrationService adminService = null;
	protected static LocationService locationService = null;
	private static final Logger logger = Logger.getLogger("SocketHandlerLogger");
	
	@Before
	public void runBeforeEachTest() throws Exception
	{
	// create the basic user and give it full rights
		initializeInMemoryDatabase();
		executeDataSet(DATASET_XML);
		 //authenticate to the temp database
		authenticate();
		
			patientService = Context.getPatientService();
		//}
	}
	/**
	 * @see {@link HL7SocketHandler#updatePatient(Patient,Patient,Date)} 
	 */
	@Test
	//@Verifies(value = "should update an existing patient", method = "updatePatient(Patient,Patient,Date)")
	public void updatePatient_shouldUpdateAnExistingPatient() throws Exception {
		
		//create the new patient
		//create the existing patient
		
			
		try{	
			Patient oldPatient = new Patient();
			PatientHandler ph = new PatientHandler();
			
			PatientService ps = Context.getPatientService();
			oldPatient = createAPatient("Jenny", "", "Patient", "9999999-7", 35, "F");
	
			Patient result = ps.savePatient(oldPatient);
			
			Assert.assertNotNull(result);
			
		}catch(Exception e){
			assertFalse(e.getMessage(), true);
		}
		
		/*Patient patient = new Patient(); 
	          patient.setGender("F"); 
	 	      patient.setPatientId(35); 
	 	      patient.addName(new PersonName("Jenny", "", "Patient")); 
	          patient.addIdentifier(new PatientIdentifier("9999999-7", new PatientIdentifierType(1), new Location(1))); 
	          patientService.savePatient(patient); */
	}
	
	
	public Patient createAPatient(String fn, String mn, String ln, 
			String mrn, Integer id,String gender){
		
		Patient patient = new Patient();
		
		try{

			Calendar dobCal = Calendar.getInstance();
			dobCal.set(2006,1,2,0,0,0);
			
			PatientIdentifierType pit = new PatientIdentifierType(3);
			PatientIdentifier pi = new PatientIdentifier();
			pi.setIdentifierType(pit);
			pi.setPatient(patient);
			pi.setIdentifier(mrn);
			patient.addIdentifier(pi);
			
			patient.setPatientId(id);
			patient.setGender(gender);
			patient.setBirthdate(dobCal.getTime());	
			PersonName name = new PersonName(fn, mn,ln);
			name.setPreferred(true);
			name.setCreator(Context.getAuthenticatedUser());
			name.setPerson(patient);
			name.setDateCreated(new Date());
			patient.addName(name);
		} catch (Exception e) {
			logger.error("Error setting new patient object. ", e);
		} 
		
		return patient;
		
		
		
		
	}
}