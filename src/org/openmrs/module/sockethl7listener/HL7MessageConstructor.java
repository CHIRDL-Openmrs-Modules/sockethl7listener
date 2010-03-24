package org.openmrs.module.sockethl7listener;

import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.sockethl7listener.service.SocketHL7ListenerService;
import org.openmrs.module.sockethl7listener.util.Util;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.v231.datatype.NM;
import ca.uhn.hl7v2.model.v25.datatype.CE;
import ca.uhn.hl7v2.model.v25.datatype.ED;
import ca.uhn.hl7v2.model.v25.datatype.ST;
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
/**
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
	private String ackType = "";
	private Socket socket = null;
	private String checkDigitScheme = "";
	private String pid2Required = "";
	private String assignAuthority = "";
	private String identifierTypeCode = "";
	private String poc = "";
	private String app_acknowledgement_type = "";
	private String processing_id = "";
	private String obsLocation = "";
	private String encoding = "";
	private String obxSubDataType = "";
	private String OBXUniversalId = "";
	private String obxDataType = "";
	private String patientClass = "";
	private boolean image = false;
	

	
	public boolean isImage() {
		return image;
	}

	public void setImage(boolean image) {
		this.image = image;
	}

	public HL7MessageConstructor(){
		
		oru = new ORU_R01();
		
	}
	
	/** Set the properties from xml in hl7configFileLoaction
	 * @param hl7configFileLocation
	 */
	public HL7MessageConstructor(String hl7configFileLocation){
		
		oru = new ORU_R01();
		setProperties(hl7configFileLocation);
			
		
	}
	
	/**
	 * Creates the PID segment of the outgoing hl7 message
	 * 
	 * @param pat
	 * @return PID
	 */
	public PID AddSegmentPID(Patient pat){
		PersonService personService = Context.getPersonService();
		
		
		PID pid = oru.getPATIENT_RESULT().getPATIENT().getPID(); 
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
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
			String assignAuthFromIdentifierType = getAssigningAuthorityFromIdentifierType(pi);
			
			
			if (pi != null){
				//Identifier PID-2 not required
				if (pid2Required != null && Boolean.valueOf(pid2Required)){
					String addon = "-" + assignAuthFromIdentifierType;
					pid.getPatientID().getIDNumber().setValue(pi.getIdentifier() + addon);
				}
			}
			
			//Identifier PID-3
			//MRN
			if (pi != null){
				String identString = pi.getIdentifier();
				if (identString != null) {
					Integer dash = identString.indexOf("-");
					if (dash >= 0){
						identString = identString.substring(0,dash) + identString.substring(dash + 1);
					}
				}
				pid.getPatientIdentifierList(0).getIDNumber().setValue(identString);
				pid.getPatientIdentifierList(0).getCheckDigitScheme().setValue(checkDigitScheme);
			}
			
			pid.getPatientIdentifierList(0).getAssigningAuthority().getNamespaceID()
				.setValue(assignAuthority);
			pid.getPatientIdentifierList(0).getIdentifierTypeCode().setValue(identifierTypeCode);
			
			//Address
			pid.getPatientAddress(0).getStreetAddress().getStreetOrMailingAddress().setValue(pat.getPersonAddress().getAddress1());
			pid.getPatientAddress(0).getOtherDesignation().setValue(pat.getPersonAddress().getAddress2());
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
	
	/**Creates the NK1 segment for hl7 message.
	 * For newborn screen, NK1 will not be included in outgoing message
	 * @param pat
	 * @return NK1 - next of kin segment
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
			pv1.getPatientClass().setValue(patientClass);
			pv1.getAttendingDoctor(0).getFamilyName().getSurname().setValue("");
			pv1.getAttendingDoctor(0).getGivenName().setValue("");

			Provider prov = new Provider();
			prov.setProviderfromUser(enc.getProvider());
			String providerId = prov.getId();
			//using npi
			if (providerId == null || providerId.equals("")) {
				String npi = hl7ListService.getNPI(prov.getFirstName(),prov.getLastName());
				providerId = npi;
			}
			
			pv1.getAttendingDoctor(0).getFamilyName().getSurname().setValue(prov.getLastName());
			pv1.getAttendingDoctor(0).getGivenName().setValue(prov.getFirstName());
			pv1.getAttendingDoctor(0).getIDNumber().setValue(providerId);
			
			String visitDate = Util.convertDateToString(enc.getEncounterDatetime());
			pv1.getAdmitDateTime().getTime().setValue(visitDate);
			pv1.getDischargeDateTime(0).getTime().setValue(visitDate);
			pv1.getVisitNumber().getIDNumber().setValue(enc.getPatient().getPatientIdentifier().getIdentifier());
			
			
			if (poc == null || poc.equals("")){
				PersonAttribute pocAttr = enc.getProvider().getAttribute("POC");
				if (pocAttr != null){
					poc = pocAttr.getValue();
					if (poc != null  && !poc.equals("")){
						pv1.getAssignedPatientLocation().getPointOfCare().setValue(poc);
						pv1.getAssignedPatientLocation().getFacility().getNamespaceID().setValue(poc);
					}
				}
			} 
			if (poc != null){
				pv1.getAssignedPatientLocation().getPointOfCare().setValue(poc);
				pv1.getAssignedPatientLocation().getFacility().getNamespaceID().setValue(poc);
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

		
		//Get current date
		String dateFormat = "yyyyMMddHHmmss";
		SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
		String formattedDate = formatter.format(new Date());
		
		try {
			msh.getFieldSeparator().setValue("|");
			msh.getEncodingCharacters().setValue("^~\\&");
			msh.getDateTimeOfMessage().getTime().setValue(formattedDate);
			if(isImage()){
				ourApplication = prop.getProperty("our_image_app");
			}
			msh.getSendingApplication().getNamespaceID().setValue(ourApplication);
			msh.getSendingFacility().getNamespaceID().setValue(ourFacility);
			msh.getMessageType().getMessageCode().setValue(messageType);
			msh.getMessageType().getTriggerEvent().setValue(triggerEvent);
			msh.getMessageControlID().setValue("");
			msh.getVersionID().getVersionID().setValue(version);
			msh.getReceivingApplication().getNamespaceID().setValue(receivingApp);
			msh.getReceivingFacility().getNamespaceID().setValue(receivingFacility);
			msh.getAcceptAcknowledgmentType().setValue(ackType);
			msh.getApplicationAcknowledgmentType().setValue(app_acknowledgement_type);
			msh.getProcessingID().getProcessingID().setValue(processing_id);
			msh.getMessageControlID().setValue("CHICA-" + formattedDate);
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
	
	
	
	public OBR AddSegmentOBR (Encounter enc, String univServiceId, 
			String univServIdName, int orderRep){
		
		
		if (univServiceId == null){
			univServiceId = this.univServId;
		}
		if (univServIdName == null){
			univServIdName = this.univServIdName;
		}
		OBR obr = null; 
		int orderObsCount = 0;

		try {
			obr = oru.getPATIENT_RESULT().getORDER_OBSERVATION(orderRep).getOBR();
			int reps = oru.getPATIENT_RESULT().getORDER_OBSERVATIONReps();
			SocketHL7ListenerService hl7ListService = Context.getService(SocketHL7ListenerService.class);
			Date encDt = enc.getEncounterDatetime();
			SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");
			SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");
			Provider prov = new Provider();
			prov.setProviderfromUser(enc.getProvider());
			String providerId = prov.getId();
			//using npi
			if (providerId == null || providerId.equals("")) {
				String npi = hl7ListService.getNPI(prov.getFirstName(),prov.getLastName());
				providerId = npi;
			}
			String encDateStr = "";
			String encDateOnly = "";
			if (encDt != null) { 
				encDateStr = df.format(encDt);
				encDateOnly = dayFormat.format(encDt);
			}
			obr.getObservationDateTime().getTime().setValue(encDateStr);
			obr.getSetIDOBR().setValue(String.valueOf(reps));
			obr.getUniversalServiceIdentifier().getIdentifier().setValue(univServiceId);
			obr.getUniversalServiceIdentifier().getText().setValue(univServIdName);
			obr.getUniversalServiceIdentifier().getNameOfCodingSystem().setValue(codeSys);
			obr.getOrderingProvider(0).getFamilyName().getSurname().setValue(prov.getLastName());
			obr.getOrderingProvider(0).getGivenName().setValue(prov.getFirstName());
			obr.getOrderingProvider(0).getIDNumber().setValue(providerId);
			obr.getResultCopiesTo(0).getFamilyName().getSurname().setValue(prov.getLastName());
			obr.getResultCopiesTo(0).getGivenName().setValue(prov.getFirstName());
			obr.getResultCopiesTo(0).getIDNumber().setValue(providerId);
			obr.getResultStatus().setValue(resultStatus);
			obr.getSpecimenActionCode().setValue(specimenActionCode);
			
			//Accession number
			String accessionNumber = String.valueOf(enc.getEncounterId()) + "-" + univServiceId 
				+ "-" + encDateOnly;
			obr.getFillerOrderNumber().getEntityIdentifier().setValue(accessionNumber);
			
		}catch (DataTypeException e) {
			Log.error("The data type was incorrect for the OBR field.", e);
		} catch (HL7Exception e) {
			Log.error("The values are the correct data type, but" 
					+ " there was an error saving values to the OBS field.", e);
		}catch (Exception e){
			Log.error(e.getMessage());
		}
		
		return obr;
		

	}
	
	
	public OBX AddSegmentOBX (String name, 
			String obsId, String obsSubId, String valueCode, String value, String units, 
			Date datetime,  String hl7Abbreviation, int orderRep, int obsRep ){
		//Observation for alert string.
		OBX obx = null;
		if (obsId == null){
			return null;
		}
		
		int obxCount = 0;
		
		try {
			//Add OBX to newest OBR
			obx = oru.getPATIENT_RESULT().getORDER_OBSERVATION(orderRep)
				.getOBSERVATION(obsRep).getOBX();
			//.getOBSERVATION() zero based, but OBX id field is not
			obx.getSetIDOBX().setValue(String.valueOf(obsRep + 1));
			obx.getValueType().setValue(hl7Abbreviation);
			obx.getUnits().getIdentifier().setValue(units);
			obx.getObservationIdentifier().getIdentifier().setValue(obsId);
			obx.getObservationIdentifier().getText().setValue(name);
			obx.getObservationSubID().setValue(obsSubId);
			obx.getObservationIdentifier().getNameOfCodingSystem().setValue(codeSys);
			String dateFormat = "yyyyMMddHHmmss";
			SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
			String formattedDate = formatter.format(datetime);
			obx.getDateTimeOfTheObservation().getTime().setValue(formattedDate);
			obx.getObservationResultStatus().setValue("F");
			obx.getProducerSID().getText().setValue(obsLocation);
			
			if (hl7Abbreviation.equals("CWE"))
			{
				//CWE cwe = new CWE(oru);
				//RMRS uses CE not CWE, so we use CE for export
				//Leaving it separated for possible future deviation. 
				CE ce = new CE(oru);
				obx.getValueType().setValue("CE");
				ce.getText().setValue(value);
				ce.getIdentifier().setValue(valueCode);
				ce.getNameOfCodingSystem().setValue(codeSys);
				obx.getObservationValue(0).setData(ce);
				
			} else if (hl7Abbreviation.equals("CE"))
			{
				CE ce = new CE(oru);
				obx.getValueType().setValue("CE");
				ce.getText().setValue(value);
				ce.getIdentifier().setValue(valueCode);
				obx.getObservationValue(0).setData(ce);
				
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
				 
				ED ed = new ED(oru);
				ed.getSourceApplication().getNamespaceID().setValue(OBXUniversalId);
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
	
	private void setProperties(String hl7ConfigFile){
		
	
		prop = Util.getProps(hl7ConfigFile);
		if (prop != null){
			codeSys = prop.getProperty("coding_system");
			checkDigitScheme = prop.getProperty("check_digit_algorithm");
			pid2Required = prop.getProperty("pid_2_required");
			assignAuthority = prop.getProperty("assigning_authority");
			identifierTypeCode = prop.getProperty("identifier_type");
			poc = prop.getProperty("point_of_care");
			ourFacility = prop.getProperty("our_facility");
			ourApplication = prop.getProperty("our_app");
			receivingApp = prop.getProperty("receiving_app");
			receivingFacility = prop.getProperty("receiving_facility");
			version = prop.getProperty("version");
			messageType = prop.getProperty("message_type");
			triggerEvent = prop.getProperty("event_type_code");
			//Acknowlegment Type AL=always; NE=never, ER= Error only, and SU=successful
			ackType = prop.getProperty("acknowledgement_type");
			univServId = prop.getProperty("univ_serv_id");
			univServIdName = prop.getProperty("univ_serv_id_name");
			codeSys = prop.getProperty("coding_system");
			resultStatus = prop.getProperty("result_status");
			specimenActionCode = prop.getProperty("specimen_action_code");
			app_acknowledgement_type = prop.getProperty("app_acknowledgement_type");
			processing_id = prop.getProperty("msh_processing_id");
			obsLocation = prop.getProperty("obs_location");
			encoding = prop.getProperty("encoding");
			obxSubDataType = prop.getProperty("OBX_sub_data_type");
			OBXUniversalId = prop.getProperty("OBX_universal_id");
			obxDataType = prop.getProperty("OBX_data_type");
			patientClass = prop.getProperty("patient_class");
		}
	
	}
	
	public ORU_R01 getOru() {
		return oru;
	}

	public void setOru(ORU_R01 oru) {
		this.oru = oru;
	}

	

}
