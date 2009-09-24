package org.openmrs.module.sockethl7listener;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sockethl7listener.service.SocketHL7ListenerService;
import org.openmrs.module.sockethl7listener.util.Util;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v22.datatype.ST;
import ca.uhn.hl7v2.model.v231.datatype.NM;
import ca.uhn.hl7v2.model.v25.datatype.ED;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.NK1;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.segment.OBX;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.model.v25.segment.PV1;
import ca.uhn.hl7v2.parser.PipeParser;



/**
 * Constructs the hl7 message resulting from an abnormal newborn screen
 * @author msheley
 *
 */
public class HL7MessageConstructor {
	
	private ORU_R01 oru;
	private String attributeNextOfKin = "Mother's Name";
	private String attributeTelephoneNum = "Telephone Number";
	private String attributeRace = "Race";
	private String ourFacility = "";
	private String ourApplication= "";
	private String univServId = "";
	private String univServIdName = "";
	private static final Logger logger = Logger.getLogger("SocketHandlerLogger");
	private Properties prop;
	private String version = "2.5";
	private String messageType = "ORU";
	private String triggerEvent = "R01";
	private String codeSys = "";
	private String receivingApp = "";
	private String receivingFacility = "";
	private String resultStatus = "";
	private String specimenActionCode;
	private Socket socket = null;
	private OutputStream os = null;
	private InputStream is = null;
	

	
	
           

	public HL7MessageConstructor(){
		
		oru = new ORU_R01();
		AdministrationService adminService = Context.getAdministrationService();
		 String hl7ConfigFile = adminService
			.getGlobalProperty("sockethl7listener.hl7ConfigFile");
		prop = Util.getProps(hl7ConfigFile);
	}
	
	/**
	 * Creates the PID segment of the outgoing hl7 message
	 * 
	 * @param pat
	 * @return PID
	 */
	public PID AddSegmentPID(Patient pat){
		PersonService personService = Context.getPersonService();
		
		codeSys = prop.getProperty("coding_system");
		String pid2Required = prop.getProperty("pid_2_required");
		PID pid = oru.getPATIENT_RESULT().getPATIENT().getPID(); 
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhhmm");
		Date dob = pat.getBirthdate();
		Date dod = pat.getDeathDate();
		String dobStr = "";
		String dodStr = "";
		if (dob != null) dobStr = df.format(dob);
		if (dod != null)dodStr = df.format(dod);

		try {
			//Name
			pid.getPatientName(0).getFamilyName().getSurname().setValue(pat.getFamilyName());
			pid.getPatientName(0).getGivenName().setValue(pat.getGivenName());
			
			//Identifiers
			PatientIdentifier pi = pat.getPatientIdentifier();
			String assignAuth = getAssigningAuthorityFromIdentifierType(pi);
			
			//Identifier PID-2 not required
			if (Boolean.valueOf(pid2Required)){
				String addon = "-" + assignAuth;
				pid.getPatientID().getIDNumber().setValue(pi.getIdentifier() + addon);
			}
			
			//Identifier PID-3
			//MRN
			pid.getPatientIdentifierList(0).getIDNumber().setValue(pi.getIdentifier());
			pid.getPatientIdentifierList(0).getAssigningAuthority().getNamespaceID()
				.setValue(assignAuth);
			
			//Address
			pid.getPatientAddress(0).getStreetAddress().getStreetOrMailingAddress().setValue(pat.getPersonAddress().getAddress1());
			pid.getPatientAddress(0).getStreetAddress().getDwellingNumber().setValue(pat.getPersonAddress().getAddress2());
			pid.getPatientAddress(0).getCity().setValue(pat.getPersonAddress().getCityVillage());
			pid.getPatientAddress(0).getStateOrProvince().setValue(pat.getPersonAddress().getStateProvince());
			pid.getPatientAddress(0).getZipOrPostalCode().setValue(pat.getPersonAddress().getPostalCode());
			
			//Telephone
			int personAttrTypeId = personService.getPersonAttributeTypeByName(attributeTelephoneNum).getPersonAttributeTypeId();
			PersonAttribute telephoneNumberAttribute = pat.getAttribute(personAttrTypeId);
			if(telephoneNumberAttribute != null)
			{
				pid.getPhoneNumberHome(0).getTelephoneNumber().setValue(telephoneNumberAttribute.getValue());
			}
			
			//gender
			pid.getAdministrativeSex().setValue(pat.getGender());
			
			//dob
			pid.getDateTimeOfBirth().getTime().setValue(dobStr);

			//Race identifier - 
			personAttrTypeId = personService.getPersonAttributeTypeByName(attributeRace).getPersonAttributeTypeId();
			PersonAttribute raceAttribute = pat.getAttribute(personAttrTypeId);
			String race = null;
			
			if(raceAttribute != null)
			{
				race = raceAttribute.getValue();
			}
			int raceID = getRaceID(race);
			if (raceID != 0){
				pid.getRace(0).getText().setValue(race);
				pid.getRace(0).getIdentifier().setValue(Integer.toString(raceID));
				pid.getRace(0).getNameOfCodingSystem().setValue(codeSys);
			}
			
			//Death 
			pid.getPatientDeathIndicator().setValue(pat.getDead().toString());
			pid.getPatientDeathDateAndTime().getTime().setValue(dodStr);
			
			//TODO: Ethnicity : OpenMRS database model does not contain a table/field for ethnicity .  
			//pid.getEthnicGroup(0).getText().setValue("");
			return pid;
			
		} catch (DataTypeException e) {
			logger.error(e);
			return null;
		} catch (HL7Exception e) {
			logger.error(e);
			return null;
		}
		
	}
	/**Constructs the entire outgoing hl7 message resulting from an abnormal newborn
	 * screen.
	 * @param enc_id
	 * @param mrn
	 * @param a --the alert message from nbs_alert table. 
	 * @return
	 */
	/*public String createHl7Message( int encounter_id, String mrn, ArrayList<Hl7Obs> hl7ObsList) {

		SocketHL7ListenerService hl7ListService = Context.getService(SocketHL7ListenerService.class);
		PatientService patientService = Context.getPatientService();
		EncounterService es = Context.getEncounterService();
		AdministrationService adminService = Context.getAdministrationService();
		ObsService obsService = Context.getObsService();
		
		
		String hl7Message = "";
		
		
		
		try {
			
			Encounter enc = es.getEncounter(encounter_id);
			if (enc == null){
				Log.error("Encounter not found for encounter_id = '" + encounter_id + "'.");
				return null;
			}
			Patient pat = patientService.getPatient(enc.getPatient().getPatientId());
			MSH msh = createMSH(enc);
			PID pid = createPID(pat);
			PV1 pv1 = createPV1(enc);
			if (pv1 == null){
				Log.error("Unable to create PV1 segment for encounter_id = '" + encounter_id + "'.");
				return null;
			}
		
			OBR obr = createOBR(enc);
	        Parser pipeParser;
			pipeParser = new PipeParser();
			OBX obx = null;
			int rep = 0;
			for (Hl7Obs hl7obs : hl7ObsList){
				rep++;
				obx = createOBX(hl7obs, rep);
			}
			hl7Message = pipeParser.encode(oru);
			hl7ListService.saveMessageToDatabase(enc, hl7Message);
			
		} catch (HL7Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();


		} catch (RuntimeException e){
			logger.error( e.getMessage());
		}
	
		return hl7Message;

	}*/
	
	/**Creates the NK1 segment for hl7 message.
	 * For newborn screen, NK1 will not be included in outgoing message
	 * @param pat
	 * @return
	 */
	public NK1 AddSegmentNK1(Patient pat) {
		NK1 nk1 = oru.getPATIENT_RESULT().getPATIENT().getNK1();
		try {
			String ln = "";
			String fn = "";
			String nkName = pat.getAttribute(attributeNextOfKin).getValue();
			if ((pat != null) && (pat.getAttribute(attributeTelephoneNum) != null)) {
				String tel = pat.getAttribute(attributeTelephoneNum).getValue();
				nk1.getPhoneNumber(0).getTelephoneNumber().setValue(tel);
			}
			if ((nkName != null) && !nkName.equals("")) {
				int indexSpace = nkName.indexOf(" ");
				if (!nkName.isEmpty()){
					if (indexSpace < 0){
						fn = nkName;
					}else {
						fn = nkName.substring(0, indexSpace -1);
						ln = nkName.substring(indexSpace +1);
					}
				}
			}
						
			nk1.getNKName(0).getFamilyName().getSurname().setValue(ln);
			nk1.getNKName(0).getGivenName().setValue(fn);
		
			// NBS temporarily set to mother
			// Original openmrs data model only collects mother's name
			// Need to update
			nk1.getRelationship().getIdentifier().setValue(
					attributeNextOfKin);
			if (pat != null) {
				PersonAddress pa = pat.getPersonAddress();
				nk1.getAddress(0).getStreetAddress()
						.getStreetOrMailingAddress().setValue(pa.getAddress1());
				nk1.getAddress(0).getStreetAddress().getDwellingNumber()
						.setValue(pa.getAddress2());
				nk1.getAddress(0).getCity().setValue(pa.getCityVillage());
				nk1.getAddress(0).getStateOrProvince().setValue(
						pa.getStateProvince());
				nk1.getAddress(0).getZipOrPostalCode().setValue(
						pa.getPostalCode());
				nk1.getAddress(0).getCountry().setValue(pa.getCountry());
			}
		} catch (DataTypeException e) {
			Log.error("DateType exception when assigning the values to the components.");
		} catch (HL7Exception e) {
			Log.error("Exception setting components of NK1.",e);
		}catch (Exception e) {
			Log.error("Exception setting next of kin",e );
		}
		return nk1;

	}
	public PV1 AddSegmentPV1(Encounter enc) {
		
		SocketHL7ListenerService hl7ListService = Context.getService(SocketHL7ListenerService.class);

		PV1 pv1 = oru.getPATIENT_RESULT().getPATIENT().getVISIT().getPV1();

		try {
			pv1.getAttendingDoctor(0).getFamilyName().getSurname().setValue("");
			pv1.getAttendingDoctor(0).getGivenName().setValue("");

			Provider prov = new Provider();
			prov.setProviderfromUser(enc.getProvider());
			String npi = hl7ListService.getNPI(prov.getFirstName(),prov.getLastName());
			if (npi == null){ 
				return null;
			}
			pv1.getAttendingDoctor(0).getFamilyName().getSurname().setValue(prov.getLastName());
			pv1.getAttendingDoctor(0).getGivenName().setValue(prov.getFirstName());
			pv1.getAttendingDoctor(0).getIDNumber().setValue(npi);
			
			pv1.getVisitNumber().getIDNumber().setValue(enc.getPatient().getPatientIdentifier().getIdentifier());
			
			PersonAttribute pocAttr = enc.getProvider().getAttribute("POC");
			if (pocAttr != null){
				String poc = pocAttr.getValue();
				pv1.getAssignedPatientLocation().getPointOfCare().setValue(poc);
			}
			
			PersonAttribute facAttr = enc.getProvider().getAttribute("POC_FACILITY");
			if (facAttr != null){
				String fac = facAttr.getValue();
				pv1.getAssignedPatientLocation().getFacility().getUniversalID().setValue(fac);
			}
			
			PersonAttribute roomAttr = enc.getProvider().getAttribute("POC_ROOM");
			if (roomAttr != null){
				String room = roomAttr.getValue();
				pv1.getAssignedPatientLocation().getRoom().setValue(room);
			}
			PersonAttribute bedAttr = enc.getProvider().getAttribute("POC_BED");
			if (bedAttr != null){
				String bed = bedAttr.getValue();
				pv1.getAssignedPatientLocation().getBed().setValue(bed);
			}
			PersonAttribute admitSource = enc.getProvider().getAttribute("ADMIT_SOURCE");
			if (admitSource != null){
				pv1.getAdmitSource().setValue(admitSource.getValue());
			}
			
		} catch (DataTypeException e) {
			logger.error("DataTypeException when constructing PV1 segment", e);
		} catch (HL7Exception e) {
			logger.error("HL7Exception when constructing PV1 segment", e);
		} catch (RuntimeException e) {
			logger.error(
					"Run time exception when constructing pv1 segment for encounter#="
							+ enc.getEncounterId(), e);
		}
		return pv1;
	}
	
	
	public MSH AddSegmentMSH(Encounter enc){
		
		MSH msh = oru.getMSH();
		//For nbs,  we are not setting the receiving facility and application
		//Location loc = enc.getLocation();
		//String locs = loc.getName();
		//String locs = locationString;
		if (prop != null){
			ourFacility = prop.getProperty("our_facility");
			ourApplication = prop.getProperty("our_app");
			receivingApp = prop.getProperty("receiving_app");
			receivingFacility = prop.getProperty("receiving_facility");
			version = prop.getProperty("version");
			messageType = prop.getProperty("message_type");
			triggerEvent = prop.getProperty("event_type_code");
			codeSys = prop.getProperty("coding_system");
			
		}
		
		
		//Get current date
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhhmm");
		Calendar cal = Calendar.getInstance();
		String currentDateTime = df.format(cal.getTime());
		
		try {
			msh.getFieldSeparator().setValue("|");
			msh.getEncodingCharacters().setValue("^~\\&");
			msh.getDateTimeOfMessage().getTime().setValue(currentDateTime);
			msh.getSendingApplication().getNamespaceID().setValue(ourApplication);
			msh.getSendingFacility().getNamespaceID().setValue(ourFacility);
			msh.getMessageType().getMessageCode().setValue(messageType);
			msh.getMessageType().getTriggerEvent().setValue(triggerEvent);
			msh.getMessageControlID().setValue("");
			msh.getVersionID().getVersionID().setValue(version);
			msh.getReceivingApplication().getNamespaceID().setValue(receivingApp);
			msh.getReceivingFacility().getNamespaceID().setValue(receivingFacility);
			
		} catch (DataTypeException e) {
			e.printStackTrace();
		}
	
		return msh;
	}
	
	private int getRaceID(String race){
		int raceID = 0; //default
		if ((race != null) && !race.equals("")){
			if (race.toLowerCase().equals("white")) raceID = 1;
			else if ( race.toLowerCase().equals("black")) raceID = 2;
			else if ( race.toLowerCase().equals("american indian")) raceID = 3;
			else if ( race.toLowerCase().equals("asian")) raceID = 4;
			else if ( race.toLowerCase().equals("other")) raceID = 5;
		}
		return raceID;
	}
	
	
	
	public OBR AddSegmentOBR (Encounter enc, String set){
		
		
		if (prop != null){
			univServId = prop.getProperty("univ_serv_id");
			univServIdName = prop.getProperty("univ_serv_id_name");
			codeSys = prop.getProperty("coding_system");
			resultStatus = prop.getProperty("result_status");
			specimenActionCode = prop.getProperty("specimen_action_code");
		}
		
		OBR obr = oru.getPATIENT_RESULT().getORDER_OBSERVATION().getOBR();

		try {
			SocketHL7ListenerService hl7ListService = Context.getService(SocketHL7ListenerService.class);
			Date encDt = enc.getEncounterDatetime();
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhhmm");
			SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");
			Provider prov = new Provider();
			prov.setProviderfromUser(enc.getProvider());
			String npi = hl7ListService.getNPI(prov.getFirstName(),prov.getLastName());
			String encDateStr = "";
			String encDateOnly = "";
			if (encDt != null) { 
				encDateStr = df.format(encDt);
				encDateOnly = dayFormat.format(encDt);
			}
			obr.getObservationDateTime().getTime().setValue(encDateStr);
			obr.getSetIDOBR().setValue("1");
			obr.getUniversalServiceIdentifier().getIdentifier().setValue(univServId);
			obr.getUniversalServiceIdentifier().getText().setValue(univServIdName);
			obr.getUniversalServiceIdentifier().getNameOfCodingSystem().setValue(codeSys);
			obr.getOrderingProvider(0).getFamilyName().getSurname().setValue(prov.getLastName());
			obr.getOrderingProvider(0).getGivenName().setValue(prov.getFirstName());
			obr.getOrderingProvider(0).getIDNumber().setValue(npi);
			obr.getResultCopiesTo(0).getFamilyName().getSurname().setValue(prov.getLastName());
			obr.getResultCopiesTo(0).getGivenName().setValue(prov.getFirstName());
			obr.getResultCopiesTo(0).getIDNumber().setValue(npi);
			obr.getResultStatus().setValue(resultStatus);
			obr.getSpecimenActionCode().setValue(specimenActionCode);
			
			//Accession number
			String accessionNumber = String.valueOf(enc.getEncounterId()) + "-" + set 
				+ "-" + encDateOnly;
			obr.getFillerOrderNumber().getEntityIdentifier().setValue(accessionNumber);
			
		}catch (DataTypeException e) {
			Log.error("The data type was incorrect for the OBR field.", e);
		} catch (HL7Exception e) {
			Log.error("The values are the correct data type, but" 
					+ " there was an error saving values to the OBS field.", e);
		}
		
		return obr;
		

	}
	
	
	public OBX AddSegmentOBX (String name, String obsId, String obsSubId, String value, String units, Date datetime,  String hl7Abbreviation, int rep ){
		//Observation for alert string.
		OBX obx = null;
		if (obsId == null){
			return null;
		}
		String encoding = prop.getProperty("encoding");
		String obsloc = prop.getProperty("obs_location");
		String codingSystem = prop.getProperty("coding_system");
		
		try {
			obx = oru.getPATIENT_RESULT().getORDER_OBSERVATION().getOBSERVATION(rep-1).getOBX();
			obx.getSetIDOBX().setValue(String.valueOf(rep));
			obx.getValueType().setValue(hl7Abbreviation);
			obx.getUnits().getText().setValue(units);
			obx.getObservationIdentifier().getIdentifier().setValue(obsId);
			obx.getObservationIdentifier().getText().setValue(name);
			obx.getObservationIdentifier().getNameOfCodingSystem().setValue(encoding);
			obx.getObservationSubID().setValue(obsSubId);
			obx.getObservationIdentifier().getNameOfCodingSystem().setValue(codingSystem);
			String dateFormat = "yyyyMMddHHmmss";
			SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
			String formattedDate = formatter.format(datetime);
			obx.getDateTimeOfTheObservation().getTime().setValue(formattedDate);
			obx.getObservationResultStatus().setValue("F");
			obx.getProducerSID().getText().setValue(obsloc);
			
			
			
			
			
			if (hl7Abbreviation.equals("CWE"))
			{
				//processCWEType(obs, message, orderRep,
				//		obxRep, pIdentifierString, stConceptId,
				//		obsValueType);
			} else if (hl7Abbreviation.equals("CE"))
			{
				
			} else if (hl7Abbreviation.equals("NM"))
			{
				NM nm = new NM(oru);
				nm.setValue(value);
				obx.getObservationValue(0).setData(nm);
				
				
			} else if (hl7Abbreviation.equals("TS"))
			{
				
			} else if (hl7Abbreviation.equals("ST"))
			{
				
				ST st = new ST(oru);
				st.setValue(value);
				obx.getObservationValue(0).setData(st);
				
			} else if (hl7Abbreviation.equals("TX"))
			{
				//okToCreateObs = processTXType(obs, message, orderRep,
				//		obxRep);
			} else if (hl7Abbreviation.equals("ED")){
				
				//The HL7 ED (Encapsulated Data) data type.  Consists of the following components: </p><ol>
				 	//Source Application (HD)</li>
					//Type of Data (ID)</li>
					//Data Subtype (ID)</li>
					//Encoding (ID)</li>
					//Data (TX)</li>
				 
				String obxValueType = prop.getProperty("OBX_value_type");
				String obxSubDataType = prop.getProperty("OBX_sub_data_type");
				
				String OBXUniversalId = prop.getProperty("OBX_universal_id");
				String obxDataType = prop.getProperty("OBX_data_type");
				String specimenActionCode = prop.getProperty("specimen_action_code");
				ED ed = new ED(oru);
				ed.getSourceApplication().getUniversalID().setValue(OBXUniversalId);
				ed.getTypeOfData().setValue(obxDataType);
				ed.getDataSubtype().setValue(obxSubDataType);
				ed.getEncoding().setValue(encoding);
				ed.getData().setValue(value);
				obx.getObservationValue(0).setData(ed);
				
				
			 }
		
		} catch (DataTypeException e) {
			logger.error("",e);
			
		} catch (HL7Exception e) {
			logger.error("",e);
			
		} catch(RuntimeException e){
			logger.error("",e);
		}
		return obx;

	}
	
	private String  getAssigningAuthorityFromIdentifierType(PatientIdentifier pi){
		String assignAuth = "";
		if (pi != null && pi.getIdentifierType() != null) {
			assignAuth = pi.getIdentifierType().getName();
			int underscore = assignAuth.indexOf('_');
			assignAuth = assignAuth.substring(underscore + 1);
		}
		return assignAuth;
		
	}
	
	public String getMessage(){
		PipeParser pipeParser = new PipeParser();
		String msg = null;
		try {
			msg = pipeParser.encode(oru);
		} catch (HL7Exception e) {
			Log.error("Exception parsing constructed message.");
		}
		return msg;

	}
	
	
	

	
}
