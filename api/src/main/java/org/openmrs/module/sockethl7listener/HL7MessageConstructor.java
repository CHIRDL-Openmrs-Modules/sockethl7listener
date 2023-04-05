package org.openmrs.module.sockethl7listener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.chirdlutil.util.IOUtil;
import org.openmrs.module.sockethl7listener.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger log = LoggerFactory.getLogger("SocketHandlerLogger");
	private ORU_R01 oru;
	private String attributeNextOfKin = "Mother's Name";
	private String attributeTelephoneNum = "Telephone Number";
	private String attributeRace = "Race";
	private String ourFacility = "";
	private String ourApplication = "";
	private String univServId = "";
	private String univServIdName = "";
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
		return this.image;
	}

	public void setImage(boolean image) {
		this.image = image;
	}

	public HL7MessageConstructor() {

		this.oru = new ORU_R01();

	}

	/**
	 * Set the properties from xml in hl7configFileLoaction
	 * 
	 * @param hl7configFileLocation
	 */
	public HL7MessageConstructor(String hl7configFileLocation) {

		this.oru = new ORU_R01();
		setProperties(hl7configFileLocation);

	}

	/**
	 * Creates the PID segment of the outgoing hl7 message
	 * 
	 * @param pat
	 * @return PID
	 */
	public PID AddSegmentPID(Patient pat) {
		
		PersonService personService = Context.getPersonService();

		PID pid = this.oru.getPATIENT_RESULT().getPATIENT().getPID();
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
		Date dob = pat.getBirthdate();
		Date dod = pat.getDeathDate();
		String dobStr = "";
		String dodStr = "";
		if (dob != null)
			dobStr = df.format(dob);
		if (dod != null)
			dodStr = df.format(dod);

		try {
			// Name
			pid.getPatientName(0).getFamilyName().getSurname().setValue(
					pat.getFamilyName());
			pid.getPatientName(0).getGivenName().setValue(pat.getGivenName());

			// Identifiers
			PatientIdentifier pi = pat.getPatientIdentifier();
			String assignAuthFromIdentifierType = getAssigningAuthorityFromIdentifierType(pi);

			if (pi != null) {
				// Identifier PID-2 not required
				if (this.pid2Required != null && Boolean.valueOf(this.pid2Required)) {
					String addon = "-" + assignAuthFromIdentifierType;
					pid.getPatientID().getIDNumber().setValue(
							pi.getIdentifier() + addon);
				}
			}

			// Identifier PID-3
			// MRN
			if (pi != null) {
				String identString = pi.getIdentifier();
				if (identString != null) {
					Integer dash = identString.indexOf("-");
					if (dash >= 0) {
						identString = identString.substring(0, dash)
								+ identString.substring(dash + 1);
					}
				}
				pid.getPatientIdentifierList(0).getIDNumber().setValue(
						identString);
				pid.getPatientIdentifierList(0).getCheckDigitScheme().setValue(
						this.checkDigitScheme);
			}

			pid.getPatientIdentifierList(0).getAssigningAuthority()
					.getNamespaceID().setValue(this.assignAuthority);
			pid.getPatientIdentifierList(0).getIdentifierTypeCode().setValue(
					this.identifierTypeCode);

			// Address
			PersonAddress personAddress = pat.getPersonAddress();
			if  (personAddress != null ){
				pid.getPatientAddress(0).getStreetAddress()
						.getStreetOrMailingAddress().setValue(
								pat.getPersonAddress().getAddress1());
				pid.getPatientAddress(0).getOtherDesignation().setValue(
						pat.getPersonAddress().getAddress2());
				pid.getPatientAddress(0).getCity().setValue(
						pat.getPersonAddress().getCityVillage());
				pid.getPatientAddress(0).getStateOrProvince().setValue(
						pat.getPersonAddress().getStateProvince());
				pid.getPatientAddress(0).getZipOrPostalCode().setValue(
						pat.getPersonAddress().getPostalCode());
			}

			// Telephone
			int personAttrTypeId = personService.getPersonAttributeTypeByName(
					this.attributeTelephoneNum).getPersonAttributeTypeId();
			PersonAttribute telephoneNumberAttribute = pat
					.getAttribute(personAttrTypeId);
			if (telephoneNumberAttribute != null) {
				pid.getPhoneNumberHome(0).getTelephoneNumber().setValue(
						telephoneNumberAttribute.getValue());
			}

			// gender
			pid.getAdministrativeSex().setValue(pat.getGender());

			// dob
			pid.getDateTimeOfBirth().getTime().setValue(dobStr);

			// Race identifier -
			personAttrTypeId = personService.getPersonAttributeTypeByName(
					this.attributeRace).getPersonAttributeTypeId();
			PersonAttribute raceAttribute = pat.getAttribute(personAttrTypeId);
			String race = null;

			if (raceAttribute != null) {
				race = raceAttribute.getValue();
			}
			int raceID = getRaceID(race);
			if (raceID != 0) {
				pid.getRace(0).getText().setValue(race);
				pid.getRace(0).getIdentifier().setValue(
						Integer.toString(raceID));
				pid.getRace(0).getNameOfCodingSystem().setValue(this.codeSys);
			}

			// Death
			pid.getPatientDeathIndicator().setValue(pat.getDead().toString());
			pid.getPatientDeathDateAndTime().getTime().setValue(dodStr);
			return pid;

		} catch (Exception e) {
			log.error("Exception adding PID segment to hl7 message for patient id {}", pid.getPatientID(), e);
			return null;
		} 
	}

	/**
	 * Creates the NK1 segment for hl7 message. For newborn screen, NK1 will not
	 * be included in outgoing message
	 * 
	 * @param pat
	 * @return NK1 - next of kin segment
	 */
	public NK1 AddSegmentNK1(Patient pat) {
		NK1 nk1 = this.oru.getPATIENT_RESULT().getPATIENT().getNK1();
		try {
			String ln = "";
			String fn = "";
			String nkName = "";
			if ((pat != null) && (pat.getAttribute(this.attributeNextOfKin) != null)) {
				nkName = pat.getAttribute(this.attributeNextOfKin).getValue();
			}
			if ((pat != null) && (pat.getAttribute(this.attributeTelephoneNum) != null)) {	
				String tel = pat.getAttribute(this.attributeTelephoneNum).getValue();
				nk1.getPhoneNumber(0).getTelephoneNumber().setValue(tel);
			}
			if ((nkName != null) && !nkName.equals("")) {
				int indexSpace = nkName.indexOf(" ");
				if (!nkName.isEmpty()) {
					if (indexSpace < 0) {
						fn = nkName;
					} else {
						fn = nkName.substring(0, indexSpace - 1);
						ln = nkName.substring(indexSpace + 1);
					}
				}
			}

			nk1.getNKName(0).getFamilyName().getSurname().setValue(ln);
			nk1.getNKName(0).getGivenName().setValue(fn);
			nk1.getRelationship().getIdentifier().setValue(this.attributeNextOfKin);
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
		
		} catch (Exception e) {
			if (pat != null) {
				log.error("Exception setting next-of-kin from hl7 NK1. Patient id = {}", pat.getPatientId(), e);
			} else {
				log.error("Exception setting next-of-kin from hl7 NK1.", e);
			}
		}
		return nk1;

	}

	/**
	 * CHICA-221 Updated method to use ProviderService and org.openmrs.Provider
	 * @param enc
	 * @return
	 */
	public PV1 AddSegmentPV1(Encounter enc) {


		PV1 pv1 = this.oru.getPATIENT_RESULT().getPATIENT().getVISIT().getPV1();

		try {
			pv1.getPatientClass().setValue(this.patientClass);
			pv1.getAttendingDoctor(0).getFamilyName().getSurname().setValue("");
			pv1.getAttendingDoctor(0).getGivenName().setValue("");

			Provider prov = new Provider();
			// CHICA-221 Use the provider that has the "Attending Provider" role for the encounter
			org.openmrs.Provider openmrsProvider = org.openmrs.module.chirdlutil.util.Util.getProviderByAttendingProviderEncounterRole(enc);
			
			if(openmrsProvider == null)
			{
				log.error("Error while creating PV1 segment. Unable to locate provider for encounter: {}", enc.getEncounterId());
				return pv1;
			}
			
			prov.setProvider(openmrsProvider);
			String providerId = prov.getEhrProviderId();
			pv1.getAttendingDoctor(0).getFamilyName().getSurname().setValue(
					prov.getLastName());
			pv1.getAttendingDoctor(0).getGivenName().setValue(
					prov.getFirstName());
			pv1.getAttendingDoctor(0).getIDNumber().setValue(providerId);

			String visitDate = Util.convertDateToString(enc
					.getEncounterDatetime());
			pv1.getAdmitDateTime().getTime().setValue(visitDate);
			pv1.getDischargeDateTime(0).getTime().setValue(visitDate);
			pv1.getVisitNumber().getIDNumber().setValue(
					enc.getPatient().getPatientIdentifier().getIdentifier());

			if (this.poc != null) {
				pv1.getAssignedPatientLocation().getPointOfCare().setValue(this.poc);
				pv1.getAssignedPatientLocation().getFacility().getNamespaceID()
						.setValue(this.poc);
			}
			
		} catch (Exception e) {
			log.error("Exception adding PV1 segment to hl7 for encounter: {}", enc.getEncounterId(), e);
		}
		return pv1;
	}

	public MSH AddSegmentMSH(Encounter enc) {

		MSH msh = this.oru.getMSH();

		// Get current date
		String dateFormat = "yyyyMMddHHmmss";
		SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
		String formattedDate = formatter.format(new Date());

		try {
			msh.getFieldSeparator().setValue("|");
			msh.getEncodingCharacters().setValue("^~\\&");
			msh.getDateTimeOfMessage().getTime().setValue(formattedDate);
			if (isImage()) {
				this.ourApplication = this.prop.getProperty("our_image_app");
			}
			msh.getSendingApplication().getNamespaceID().setValue(
					this.ourApplication);
			msh.getSendingFacility().getNamespaceID().setValue(this.ourFacility);
			msh.getMessageType().getMessageCode().setValue(this.messageType);
			msh.getMessageType().getTriggerEvent().setValue(this.triggerEvent);
			msh.getMessageControlID().setValue("");
			msh.getVersionID().getVersionID().setValue(this.version);
			msh.getReceivingApplication().getNamespaceID().setValue(
					this.receivingApp);
			msh.getReceivingFacility().getNamespaceID().setValue(
					this.receivingFacility);
			msh.getAcceptAcknowledgmentType().setValue(this.ackType);
			msh.getApplicationAcknowledgmentType().setValue(
					this.app_acknowledgement_type);
			msh.getProcessingID().getProcessingID().setValue(this.processing_id);
			msh.getMessageControlID().setValue(
					this.ourApplication + "-" + formattedDate);
		} catch (Exception e) {
			log.error("Exception constructing MSH segment for export message. EncounterId: {} ", enc.getEncounterId(), e);
		}

		return msh;
	}

	private int getRaceID(String race) {
		int raceID = 0; // default
		if ((race != null) && !race.equals("")) {
			if (race.equalsIgnoreCase("white"))
				raceID = 1;
			else if (race.equalsIgnoreCase("black"))
				raceID = 2;
			else if (race.equalsIgnoreCase("american indian"))
				raceID = 3;
			else if (race.equalsIgnoreCase("asian"))
				raceID = 4;
			else if (race.equalsIgnoreCase("other"))
				raceID = 5;
		}
		return raceID;
	}

	/**
	 * CHICA-221 Updated method to use ProviderService and org.openmrs.Provider
	 * @param enc
	 * @param univServiceId
	 * @param univServIdName
	 * @param orderRep
	 * @return
	 */
	public OBR AddSegmentOBR(Encounter enc, String univServiceId,
			String univServIdName, int orderRep) {

		if (univServiceId == null) {
			univServiceId = this.univServId;
		}
		if (univServIdName == null) {
			univServIdName = this.univServIdName;
		}
		OBR obr = null;

		try {
			obr = this.oru.getPATIENT_RESULT().getORDER_OBSERVATION(orderRep)
					.getOBR();
			int reps = this.oru.getPATIENT_RESULT().getORDER_OBSERVATIONReps();
			Date encDt = enc.getEncounterDatetime();
			SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");
			SimpleDateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");
			Provider prov = new Provider();
			
			// CHICA-221 Use the provider that has the "Attending Provider" role for the encounter
			org.openmrs.Provider openmrsProvider = org.openmrs.module.chirdlutil.util.Util.getProviderByAttendingProviderEncounterRole(enc);
			
			if(openmrsProvider == null)
			{
				log.error("Error setting attending provider in OBR segment for encounter {} ", enc.getEncounterId());
				return obr;
			}
			
			prov.setProvider(openmrsProvider);
			String providerId = prov.getEhrProviderId();
			String encDateStr = "";
			String encDateOnly = "";
			if (encDt != null) {
				encDateStr = df.format(encDt);
				encDateOnly = dayFormat.format(encDt);
			}
			obr.getObservationDateTime().getTime().setValue(encDateStr);
			obr.getSetIDOBR().setValue(String.valueOf(reps));
			obr.getUniversalServiceIdentifier().getIdentifier().setValue(
					univServiceId);
			obr.getUniversalServiceIdentifier().getText().setValue(
					univServIdName);
			obr.getUniversalServiceIdentifier().getNameOfCodingSystem()
					.setValue(this.codeSys);
			obr.getOrderingProvider(0).getFamilyName().getSurname().setValue(
					prov.getLastName());
			obr.getOrderingProvider(0).getGivenName().setValue(
					prov.getFirstName());
			obr.getOrderingProvider(0).getIDNumber().setValue(providerId);
			obr.getResultCopiesTo(0).getFamilyName().getSurname().setValue(
					prov.getLastName());
			obr.getResultCopiesTo(0).getGivenName().setValue(
					prov.getFirstName());
			obr.getResultCopiesTo(0).getIDNumber().setValue(providerId);
			obr.getResultStatus().setValue(this.resultStatus);
			obr.getSpecimenActionCode().setValue(this.specimenActionCode);

			// Accession number
			String accessionNumber = String.valueOf(enc.getEncounterId()) + "-"
					+ univServiceId + "-" + encDateOnly;
			obr.getFillerOrderNumber().getEntityIdentifier().setValue(
					accessionNumber);

		} catch (Exception e) {
			log.error("Exception adding OBR segment to hl7. EncounterId: {} " + enc.getEncounterId(), e);
		}

		return obr;

	}

	/**
	 * @param name (RMRS) Concept name
	 * @param obsId (RMRS) Concept code for obs
	 * @param obsSubId
	 * @param valueCode (RMRS) Concept code for value
	 * @param value  Concept answer value
	 * @param units  Units for value
	 * @param datetime observation datetime
	 * @param hl7Abbreviation HL7 Abbreviation for data type (NM, ST, TS..)
	 * @param orderRep (OBR repetition number)
	 * @param obsRep (OBS repetition number)
	 * @return
	 */
	public OBX AddSegmentOBX(String name, String obsId, String obsSubId,
			String valueCode, String value, String units, Date datetime,
			String hl7Abbreviation, int orderRep, int obsRep) {
		// Observation for alert string.
		OBX obx = null;
		if (obsId == null) {
			return null;
		}

		try {
			// Add OBX to newest OBR
			obx = this.oru.getPATIENT_RESULT().getORDER_OBSERVATION(orderRep)
					.getOBSERVATION(obsRep).getOBX();
			// .getOBSERVATION() zero based, but OBX id field is not
			obx.getSetIDOBX().setValue(String.valueOf(obsRep + 1));
			obx.getValueType().setValue(hl7Abbreviation);
			obx.getUnits().getIdentifier().setValue(units);
			obx.getObservationIdentifier().getIdentifier().setValue(obsId);
			obx.getObservationIdentifier().getText().setValue(name);
			obx.getObservationSubID().setValue(obsSubId);
			obx.getObservationIdentifier().getNameOfCodingSystem().setValue(
					this.codeSys);
			String dateFormat = "yyyyMMddHHmmss";
			SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
			String formattedDate = formatter.format(datetime);
			obx.getDateTimeOfTheObservation().getTime().setValue(formattedDate);
			obx.getObservationResultStatus().setValue("F");
			obx.getProducerSID().getText().setValue(this.obsLocation);

			if (hl7Abbreviation.equals("CWE")) {
				// RMRS uses CE not CWE, so we use CE for export
				// Leaving it separated for possible future deviation.
				CE ce = new CE(this.oru);
				obx.getValueType().setValue("CE");
				ce.getText().setValue(value);
				ce.getIdentifier().setValue(valueCode);
				ce.getNameOfCodingSystem().setValue(this.codeSys);
				obx.getObservationValue(0).setData(ce);

			} else if (hl7Abbreviation.equals("CE")) {
				CE ce = new CE(this.oru);
				obx.getValueType().setValue("CE");
				ce.getText().setValue(value);
				ce.getIdentifier().setValue(valueCode);
				obx.getObservationValue(0).setData(ce);

			} else if (hl7Abbreviation.equals("NM")) {
				NM nm = new NM(this.oru);
				nm.setValue(value);
				obx.getObservationValue(0).setData(nm);

			} else if (hl7Abbreviation.equals("TS")) {

			} else if (hl7Abbreviation.equals("ST")) {

				ST st = new ST(this.oru);
				st.setValue(value);
				obx.getObservationValue(0).setData(st);

			} else if (hl7Abbreviation.equals("TX")) {
				// okToCreateObs = processTXType(obs, message, orderRep,
				// obxRep);
			} else if (hl7Abbreviation.equals("ED")) {

				// The HL7 ED (Encapsulated Data) data type. Consists of the
				// following components: </p><ol>
				// Source Application (HD)</li>
				// Type of Data (ID)</li>
				// Data Subtype (ID)</li>
				// Encoding (ID)</li>
				// Data (TX)</li>

				ED ed = new ED(this.oru);
				ed.getSourceApplication().getNamespaceID().setValue(
						this.OBXUniversalId);
				ed.getDataSubtype().setValue(this.obxSubDataType);
				if (this.obxSubDataType != null
						&& (this.obxSubDataType.equalsIgnoreCase("TIFF")
								|| this.obxSubDataType.equalsIgnoreCase("TIF")
								|| this.obxSubDataType.equalsIgnoreCase("PDF") || this.obxSubDataType
								.equalsIgnoreCase("jpg"))) {
					ed.getTypeOfData().setValue("image");
				} else {
					ed.getTypeOfData().setValue(this.obxDataType);
				}
				ed.getEncoding().setValue(this.encoding);
				ed.getData().setValue(value);
				obx.getObservationValue(0).setData(ed);

			}

		} catch (Exception e) {
			log.error("Exception constructing OBX segment for concept {}", name ,e);
		}
		return obx;

	}

	public String getAssigningAuthorityFromIdentifierType(PatientIdentifier pi) {
		String assignAuth = "";
		if (pi != null && pi.getIdentifierType() != null) {
			assignAuth = pi.getIdentifierType().getName();
			int underscore = assignAuth.indexOf('_');
			assignAuth = assignAuth.substring(underscore + 1);
		}
		return assignAuth;

	}

	public String getMessage() {
		PipeParser pipeParser = new PipeParser();
		String msg = null;
		try {
			msg = pipeParser.encode(this.oru);
		} catch (Exception e) {
			log.error("Exception parsing constructed message.", e);
		}
		return msg;

	}

	private void setProperties(String hl7ConfigFile) {

		this.prop = IOUtil.getProps(hl7ConfigFile);
		if (this.prop != null) {
			this.codeSys = this.prop.getProperty("coding_system");
			this.checkDigitScheme = this.prop.getProperty("check_digit_algorithm");
			this.pid2Required = this.prop.getProperty("pid_2_required");
			this.assignAuthority = this.prop.getProperty("assigning_authority");
			this.identifierTypeCode = this.prop.getProperty("identifier_type");
			this.poc = this.prop.getProperty("point_of_care");
			this.ourFacility = this.prop.getProperty("our_facility");
			this.ourApplication = this.prop.getProperty("our_app");
			this.receivingApp = this.prop.getProperty("receiving_app");
			this.receivingFacility = this.prop.getProperty("receiving_facility");
			this.version = this.prop.getProperty("version");
			this.messageType = this.prop.getProperty("message_type");
			this.triggerEvent = this.prop.getProperty("event_type_code");
			// Acknowledgment Type AL=always; NE=never, ER= Error only, and
			// SU=successful
			this.ackType = this.prop.getProperty("acknowledgement_type");
			this.univServId = this.prop.getProperty("univ_serv_id");
			this.univServIdName = this.prop.getProperty("univ_serv_id_name");
			this.codeSys = this.prop.getProperty("coding_system");
			this.resultStatus = this.prop.getProperty("result_status");
			this.specimenActionCode = this.prop.getProperty("specimen_action_code");
			this.app_acknowledgement_type = this.prop
					.getProperty("app_acknowledgement_type");
			this.processing_id = this.prop.getProperty("msh_processing_id");
			this.obsLocation = this.prop.getProperty("obs_location");
			this.encoding = this.prop.getProperty("encoding");
			this.obxSubDataType = this.prop.getProperty("OBX_sub_data_type");
			this.OBXUniversalId = this.prop.getProperty("OBX_universal_id");
			this.obxDataType = this.prop.getProperty("OBX_data_type");
			this.patientClass = this.prop.getProperty("patient_class");
		}

	}

	public ORU_R01 getOru() {
		return this.oru;
	}

	public void setOru(ORU_R01 oru) {
		this.oru = oru;
	}

	public String getAssignAuthority() {
		return this.assignAuthority;
	}

	public void setAssignAuthority(String assignAuthority) {
		this.assignAuthority = assignAuthority;
	}
	public void setAssignAuthority(PatientIdentifier pi) {
		
		this.assignAuthority = getAssigningAuthorityFromIdentifierType(pi);
	}
}
