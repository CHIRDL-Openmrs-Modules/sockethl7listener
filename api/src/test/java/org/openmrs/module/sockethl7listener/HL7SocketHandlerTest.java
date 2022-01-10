package org.openmrs.module.sockethl7listener;



import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Calendar;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PersonName;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sockethl7listener.hibernateBeans.PatientMessage;
import org.openmrs.module.sockethl7listener.service.SocketHL7ListenerService;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;

public class HL7SocketHandlerTest extends BaseModuleContextSensitiveTest{
	
	protected static final String DATASET_XML = "dbunit/BasicTest.xml";
	private static final Logger logger = Logger.getLogger("SocketHandlerLogger");
	
	@BeforeEach
	public void runBeforeEachTest() throws Exception
	{
	    // create the basic user and give it full rights
		initializeInMemoryDatabase();
		executeDataSet(DATASET_XML);
		 //authenticate to the temp database
		authenticate();
	}
	
	/**
	 * @see HL7SocketHandler#updatePatient(Patient mp, Patient hl7Patient,Date encounterDate, HashMap<String,Object> parameters) 
	 */
	@Test
	public void testUpdateAnExistingPatient() throws Exception {

		Patient patient = new Patient();
		PatientService patientSerivce = Context.getPatientService();
		
		patient.setGender("F");
		patient.setPatientId(35);
		patient.addName(new PersonName("Jenny", "A", "Patient"));
		patient.addIdentifier(new PatientIdentifier("1313-4", new PatientIdentifierType(1), new Location(1)));
		Patient savedPatient = patientSerivce.savePatient(patient);
		assertNotNull(savedPatient);
		

		Patient newPatient = new Patient();
		newPatient.setGender("F");
		newPatient.setPatientId(999);
		newPatient.addName(new PersonName("Ima", "", "Patient"));
		newPatient.addIdentifier(new PatientIdentifier("1212-0", new PatientIdentifierType(1), new Location(1)));
		newPatient = patientSerivce.savePatient(newPatient);
		assertNotNull(newPatient);
		newPatient.addName(new PersonName("Jane", "", "Patient"));
		HL7SocketHandler hl7SocketHander = new HL7SocketHandler();
		Calendar cal = Calendar.getInstance();

		Patient updatedPatient = hl7SocketHander.updatePatient(patient, newPatient, cal.getTime(), null);
		assertNotNull(updatedPatient);

	}
	
	@Test
	public void testGetPatientMessageByIdentifier() throws Exception {

		SocketHL7ListenerService service = Context.getService(SocketHL7ListenerService.class);
		PatientMessage message = service.getPatientMessageByEncounter(1);
		assertNotNull(message);
		
	}
	
}
