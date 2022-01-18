package org.openmrs.module.sockethl7listener;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptName;
import org.openmrs.ConceptNumeric;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.api.context.Daemon;
import org.openmrs.hl7.HL7Constants;
import org.openmrs.module.chirdlutil.util.ChirdlUtilConstants;
import org.openmrs.module.chirdlutil.util.DateUtil;
import org.openmrs.module.sockethl7listener.hibernateBeans.HL7Outbound;
import org.openmrs.module.sockethl7listener.service.SocketHL7ListenerService;
import org.openmrs.module.sockethl7listener.util.Util;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.app.Application;
import ca.uhn.hl7v2.app.ApplicationException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.NTE;
import ca.uhn.hl7v2.util.Terser;

/**
 * 
 * 
 */
@SuppressWarnings("deprecation")
public class HL7SocketHandler implements Application {
	
	private static final Logger log =  LoggerFactory.getLogger("SocketHandlerLogger");
    private static final String DATE_FORMAT_YYYY_MM_DD_HH_MM = "yyyyMMddHHmm";
	private static final String DATE_FORMAT_YYYY_MM_DD_HH_MM_SS = "yyyyMMddHHmmss";
	private static final String DATE_FORMAT_YYYY_MM_DD = "yyyyMMdd";
		
	protected PatientHandler patientHandler;
	protected HL7ObsHandler hl7ObsHandler = null;
	protected HL7EncounterHandler hl7EncounterHandler = null;
	protected HL7PatientHandler hl7PatientHandler = null;
	private Integer port;
	private String host;
	private Socket socket = null;
	private OutputStream os = null;
	private InputStream is = null;
	
	protected ca.uhn.hl7v2.parser.Parser parser = null;
	private ArrayList<HL7Filter> filters = null;
	
	public HL7SocketHandler(){
		
		if (this.port == null){
			this.port = 0;
		}
		if (this.host == null){
			this.host = "localhost";
		}
		
	}
	
	public HL7SocketHandler(ca.uhn.hl7v2.parser.Parser parser,
			PatientHandler patientHandler, HL7ObsHandler hl7ObsHandler,
			HL7EncounterHandler hl7EncounterHandler,
			HL7PatientHandler hl7PatientHandler,
			List<HL7Filter> filters)
	{
		
		this.patientHandler = patientHandler;
		this.parser = parser;
		this.hl7ObsHandler = hl7ObsHandler;
		this.hl7EncounterHandler = hl7EncounterHandler;
		this.hl7PatientHandler = hl7PatientHandler;
		this.filters = (ArrayList<HL7Filter>) filters;
	}
	
	/**
	 * Returns true if the message is not null and is an instance of ADT_A01 (which A04 and A08 are since hapi
	 *  uses the same message structure for all A0x messages)
	 * 
	 * @returns true
	 */
	public boolean canProcess(Message message) {
		return message instanceof ca.uhn.hl7v2.model.v25.message.ADT_A01;
	}

	/**
	 * Processes incoming hl7 messages based on message type. Creates and returns an ACK message response.
	 * @param message
	 * @param parameters
	 * @return
	 */
	protected Message processMessage(Message message, HashMap<String, Object> parameters) throws ApplicationException { 
		ProcessMessageRunnable processMessageRunnable = new ProcessMessageRunnable(message, this, 
			this.hl7EncounterHandler, this.parser, this.filters, parameters);
		Thread messageProcessThread = 
				Daemon.runInDaemonThread(processMessageRunnable, Util.getDaemonToken());
		try {
			messageProcessThread.join();
		}
		catch (InterruptedException e) {
			log.error("Process message thread interrupted.", e);
			Thread.currentThread().interrupt();
		}
		
		Exception exception = processMessageRunnable.getException();
		if (exception instanceof ApplicationException) {
			throw (ApplicationException)exception;
		}
		
		return processMessageRunnable.getResult();
	}
	
	/**
	 * Processes an ORU R01 event message
	 * 
	 * @throws ContextAuthenticationException
	 */
	public Message processMessage(Message message) throws ApplicationException {
		
		HashMap<String,Object> parameters = new HashMap<String,Object>();
		return this.processMessage(message, parameters);
	}

	public Encounter checkin(Provider provider,Patient patient,Date encounterDate,
			Message message,
			String incomingMessageString,
			Encounter newEncounter,HashMap<String,Object> parameters){
		
		SocketHL7ListenerService hl7ListService = Context.getService(SocketHL7ListenerService.class);
		PatientService patientService = Context.getPatientService();
		if (provider.createProvider(provider) == null){
			log.error("Could not create Provider object from provider {}",  provider.getEhrProviderId());
			return null;
		}
		 
		Patient resultPatient = findPatient(patient,encounterDate,parameters);	
		if (resultPatient == null || resultPatient.getPatientId() == null){
			hl7ListService.setHl7Message(0, 0, incomingMessageString, false, false, this.port);
			return null;
		}
		
		Patient pat = patientService.getPatient(resultPatient.getPatientId());
		
		Encounter encounter = processEncounter(incomingMessageString,pat,
					encounterDate, newEncounter, provider,parameters);
		
		// CHICA-1157
		// Use this to determine if we should return from here or continue on, 
		// which will run TriggerPatientAfterAdvice when .messageProcessed() is called below
		// This parameter will be set to false when we already had an encounter for the day,
		// possibly created from the A10 message that was converted to an A04
		boolean newEncounterCreated = true;
		Object newEncounterCreatedObject = parameters.get(ChirdlUtilConstants.PARAMETER_NEW_ENCOUNTER_CREATED);
		if(newEncounterCreatedObject instanceof Boolean)
		{
			newEncounterCreated = (boolean)newEncounterCreatedObject;
		}
		
		if (encounter == null || !newEncounterCreated) return null;
		
		int reps = this.hl7ObsHandler.getReps(message);// number of obs 

		if (message instanceof ORU_R01)
		{
			for (int rep = 0; rep < reps; rep++)
			{
				CreateObservation(encounter, true,message,0,rep,encounter.getLocation(),resultPatient);
			}
		}
			
		// trigger rules for NBS module and ATD module
		SocketHL7ListenerService socketHL7ListenerService = Context.getService(SocketHL7ListenerService.class);
		socketHL7ListenerService.messageProcessed(encounter, parameters);
		
		return encounter;
	}

	protected boolean processMessageSegments(Message message,String incomingMessageString,HashMap<String,Object> parameters) throws HL7Exception {
		SocketHL7ListenerService hl7ListService = Context.getService(SocketHL7ListenerService.class);
		
		EncounterService encounterService = Context.getEncounterService();
		AdministrationService adminService = Context.getAdministrationService();
		// validate message
		Date starttime = new Date();
		validate(message);
		boolean error = false;

		MSH msh = HL7ObsHandler25.getMSH(message);
		parameters.put(ChirdlUtilConstants.PARAMETER_SENDING_APPLICATION, msh.getSendingApplication().getNamespaceID().getValue());
		parameters.put(ChirdlUtilConstants.PARAMETER_SENDING_FACILITY, msh.getSendingFacility().getNamespaceID().getValue());
		
		// CHICA-1185 Determine if this message was an A10 that was converted to an A04
		HL7EventTypeHandler hl7EventHandler = new HL7EventTypeHandler25();
		String eventTypeCode = hl7EventHandler.getEventTypeCode(message);
		parameters.put(ChirdlUtilConstants.PARAMETER_HL7_EVENT_TYPE_CODE, eventTypeCode);
		
		// Obtain message control id (unique ID for message from sending
		// application)
		String messageControlId = msh.getMessageControlID().getValue();
		
		try {
			//Check for string match and store message 
			//with pid and encounter id from the matched message entry
			//with duplicate flag indicated
			boolean isDuplicateHL7 = false;
			 String checkDuplicates = adminService
				.getGlobalProperty("sockethl7listener.checkForDuplicates");
			if (checkDuplicates != null && Boolean.valueOf(checkDuplicates)){
				isDuplicateHL7 = hl7ListService.checkMD5(incomingMessageString, this.port);
			}
		
			//If duplicate message string, do not process 
			if (! isDuplicateHL7){
				
				String locationString = this.hl7EncounterHandler.getLocation(message); // CHICA-982 Get location from PV1 instead of ZLR segment
				Location encounterLocation = setLocation(locationString);
				Date encounterDate = this.hl7EncounterHandler.getEncounterDate(message);
				Patient hl7Patient = this.patientHandler.setPatientFromHL7(message,encounterDate,encounterLocation,this.hl7PatientHandler, parameters); // CHICA-1185 Add parameters
				
				// Extract provider information for provider observations.
				Provider provider = this.hl7EncounterHandler.getProvider(message);

				if (provider != null){
					Encounter newEncounter = new Encounter();
					newEncounter.setLocation(encounterLocation);
					newEncounter.setEncounterDatetime(encounterDate);
					EncounterType encType = encounterService.getEncounterType("HL7Message");
					if (encType == null){
						encType = new EncounterType("HL7Message", "Arrival from hl7 message.");
						encType = encounterService.saveEncounterType(encType);
					}
						
					newEncounter.setEncounterType(encType);
		
					checkin(provider, hl7Patient, encounterDate,
							message, incomingMessageString, newEncounter,parameters);
				} else error = true;
			}
				
			double duration =  (new Date().getTime() - starttime.getTime())/1000.0;
			log.debug(String.format("Message process time: %f seconds. ", duration));
			
		} catch (RuntimeException e) {
			//Do not stop application. Start processing next hl7 message.
			log.error("RuntimeException processing ORU_RO1", e);
			error = true;
		} 
		return error;
	}

	
	/**
	 * Creates new patient or updates existing patient based on patient matching
	 * results.
	 * 
	 * @return
	 */

	@SuppressWarnings("deprecation")
	protected Patient findPatient(Patient hl7Patient
			,Date encounterDate,HashMap<String,Object> parameters
			) {

		PatientService patientService = Context.getPatientService();
		Patient resultPatient = new Patient();
		
		try {
			Patient matchedPatient = patientService.getPatientByExample(hl7Patient); // CHICA-1151 Replace findPatient() with getPatientByExample()
			if (matchedPatient == null) {
				resultPatient = createPatient(hl7Patient);
			}
			else {
				resultPatient = updatePatient(matchedPatient,
						hl7Patient,encounterDate, parameters); // CHICA-1185 Add parameters
			}
			
			
		} catch (RuntimeException e) {
			log.error(String.format("Exception creating or updating patient %d ", resultPatient.getPatientId()),e);
		}
		return resultPatient;

	}

	/**
	 * Create encounter from patient, provider,location, datetime. Create
	 * observation containing provider information.
	 * 
	 * CHICA-221 Updated method to use org.openmrs.Provider
	 * 
	 * @param pid
	 * @param pv1
	 * @param obr
	 * @param msh
	 * @return
	 */

	protected Encounter createEncounter(Patient resultPatient,
			Encounter newEncounter,Provider provider,
			HashMap<String,Object> parameters)
	{

		EncounterService es = Context.getEncounterService();
		try
		{

			if (resultPatient != null)
			{
				org.openmrs.Provider openmrsProvider = provider.getProvider(provider);
				// CHICA-221 Use the new setProvider() method
				EncounterRole encounterRole = es.getEncounterRoleByName(ChirdlUtilConstants.ENCOUNTER_ROLE_ATTENDING_PROVIDER);
				newEncounter.setProvider(encounterRole, openmrsProvider);
				newEncounter.setPatient(resultPatient);
				// newEncounter.setPatient(resultPatient); // CHICA-1151 remove call to setPatientId() the setPatient() method called above already does this
				es.saveEncounter(newEncounter);
				return newEncounter;
			}
		} catch (Exception e)
		{
			log.error(" createEncounter() or createUser() api failed. ", e);
			return null;

		}
		return null;

	}

	/**
	 * Parses observation fields from OBX segments and inserts into database for
	 * each encounter
	 * 
	 * @param msh
	 * @param d
	 * @param obx
	 * @param obr
	 * @param enc
	 * @param rep
	 * @return
	 */
	public Obs CreateObservation(Encounter enc, boolean saveToDatabase,Message message,
			int orderRep,int obxRep,Location existingLoc,Patient resultPatient) {
		ObsService os = Context.getObsService();

		String pIdentifierString = resultPatient.getPatientIdentifier().getIdentifier();
		
		if (!saveToDatabase){
			return new Obs();
		}
	
		// Get obsv type
		String obsValueType = this.hl7ObsHandler.getObsValueType(message, orderRep,obxRep);
		if (obsValueType == null) {
			return new Obs();
		}
		
		// set Observation date/time
		//Modified to check for instance of ORU_RO1
		//Reason: Previously, the only messages that had OBX
		//were ORU_R01.  However, some projects have ADT
		//messages with observations. The method isNTE() assumes
		//messages as ORU, so check first. 
		
		//MES CHICA-358 Use OBR datetime if OBX is null
		Date obsDateTime = this.hl7ObsHandler.getObsDateTime(message, orderRep, obxRep);
		
		try {
			if (obsDateTime == null && (message instanceof ca.uhn.hl7v2.model.v25.message.ORU_R01 || message instanceof ca.uhn.hl7v2.model.v23.message.ORU_R01)){ // DWE CHICA-556 Make sure this is an ORU_R01 message before trying to use the OBR date/time
				if (enc == null && isNTE(message,orderRep, obxRep)){
					obsDateTime = new Date();
				}else {
					if(enc != null){
						obsDateTime = enc.getEncounterDatetime();
					}
					else{
						Terser terser = new Terser(message);
						String datetime = terser.get("/.ORDER_OBSERVATION(" + orderRep + ")/OBR-7-1");
						if (datetime != null && datetime.length() == 8){
							obsDateTime = DateUtil.parseDate(datetime, DATE_FORMAT_YYYY_MM_DD);
						} else if (datetime != null && datetime.length() == 12){
							obsDateTime = DateUtil.parseDate(datetime, DATE_FORMAT_YYYY_MM_DD_HH_MM);
						} else if (datetime != null && datetime.length() == 14){
							obsDateTime = DateUtil.parseDate(datetime, DATE_FORMAT_YYYY_MM_DD_HH_MM_SS);
						} else {
							log.error("Invalid date / time {}", datetime );
							return new Obs();
						}
					}
				}
			}
		} catch (Exception e){
			log.error("Exception getting obs datetime from OBX/OBR.", e);
			return new Obs();
		}
		
		Obs obs = new Obs();
		obs.setObsDatetime(obsDateTime);

		// set Location
		Location loc = new Location();
		String sendingFacility = this.hl7ObsHandler.getSendingFacility(message);
		loc.setName(sendingFacility);
		try {
			if (existingLoc != null) {
				obs.setLocation(existingLoc);
			} else {
				obs.setLocation(loc);
			}
		} catch (APIException e) {
			log.error("Unable to set location.", e);
		}
		
		obs.setPerson(resultPatient);
		
		// Set the encounter based on the encounter id
		obs.setEncounter(enc);
		
		String conceptName = null;

		// Get observation question/concept from field 3 Always CE type
		// Assumption that first field is always a string representing an
		// integer

		String stConceptId = this.hl7ObsHandler.getConceptId(message, orderRep, obxRep);
		int conceptId = 0;

		// if the conceptId isn't there, lookup the code by name
		conceptName = this.hl7ObsHandler.getConceptName(message, orderRep, obxRep);
		try{
			conceptId = Integer.parseInt(stConceptId);
		} catch (NumberFormatException e){
			log.error( "Unable to parse integer from OBX", e);
			return obs;
		}
		
		
		Concept concept = Util.lookupConcept(conceptId, conceptName);
		
		if(concept == null){
			return obs;
		}
		
		// DWE CHICA-635
		if(HL7Constants.HL7_NUMERIC.equals(obsValueType)){
			concept = new ConceptNumeric();
		}
		concept.setConceptId(conceptId);
		ConceptName name = new ConceptName();
		name.setName(conceptName);
		name.setLocale(new Locale("en_US"));
		concept.addName(name);		
		obs.setConcept(concept);

		// Meeting on 3/20/07 - only one set of data in field 5. There will be
		// no "~"
		// Varies value = values[0];
	
		if (obsValueType.equals("CWE")  && !processCWEType(obs, message, orderRep,
					obxRep, pIdentifierString, stConceptId,obsValueType)){
				return obs;
		}
		if (obsValueType.equals("CE")  && !processCEType(obs, message, orderRep,
				obxRep, pIdentifierString, stConceptId,obsValueType)){
			return obs;
		}
		
		if (obsValueType.equals("NM")  && !processNMType(obs, message, orderRep,obxRep)){
			return obs;
		}
		if (obsValueType.equals("TS")  && !processTSType(obs, message, orderRep,obxRep)){
			return obs;
		}
		if (obsValueType.equals("ST")  && !processSTType(obs, message, orderRep,obxRep)){
			return obs;
		}
		if (obsValueType.equals("TX")  && !processTXType(obs, message, orderRep,obxRep)){
			return obs;
		}
		
		//create the obs
		os.saveObs(obs,null);
		if(enc != null){
		    enc.addObs(obs);
			
		}
			
		return obs;

	}
	
	private void validate(Message message) throws HL7Exception {
		// TODO: check version, etc.
	}

	
	public Location setSendingFacility(Message message) {
		
	    LocationService locationService = Context.getLocationService();
		String sendingLocString = null;
		Location existingLocation = new Location();
		Location newLocation = new Location();

		try {
			sendingLocString = this.hl7ObsHandler.getSendingFacility(message);
			existingLocation = locationService.getLocation(sendingLocString);
			if (existingLocation != null) {
				return existingLocation;
			} 
				newLocation.setName(sendingLocString);
				locationService.saveLocation(newLocation);
				return newLocation;
		} catch (Exception e) {
			log.warn("Unable to parse the sending facility location from MSH. Message:",e);
		}
		
		return null;
	}
	
	protected Patient createPatient(Patient p){
		
		PatientService patientService = Context.getPatientService();
		return patientService.savePatient(p);
	}
	
	protected Patient updatePatient(Patient mp,
			Patient hl7Patient,Date encounterDate, HashMap<String,Object> parameters){ // CHICA-1185 Add parameters
	
		PatientService patientService = Context.getPatientService();
		MatchHandler matchHandler = new MatchHandler();
		
		Patient matchPatient = patientService.getPatient(mp.getPatientId());
		
		Patient resolvedPatient = matchHandler.setPatient(hl7Patient, matchPatient, encounterDate);
		
		if (resolvedPatient == null ){
			return null;
		}
		Patient resultPatient = new Patient();
		try {
			resultPatient = patientService.savePatient(resolvedPatient);
		} catch (APIException e) {
			log.error("Exception saving matched patient.",e);
		}
		return resultPatient;
	}
	
	protected Location setLocation(String locCode){
		 LocationService locationService = Context.getLocationService();
		 Location location = new Location ();
		 Location loc  = locationService.getLocation(locCode);
		 Date date = new Date();
		 
		 if (loc == null) {
			Location inpcloc = locationService.getLocation("Unknown Location");
		    if (inpcloc == null) {
		    	//this is the default. Create if not present
		    	location.setName("Unknown Location");
		    	location.setDateCreated(date);
		    	location.setCreator(Context.getAuthenticatedUser());
		    	locationService.saveLocation(location);
		    	loc = location;
		    } else {
		    	loc = inpcloc;
		    }
		 }
		 
		return loc;
	}
	
	/**
	 * CHICA-221 Updated method to use ProviderService and org.openmrs.Provider
	 * @param incomingMessageString
	 * @param p
	 * @param encDate
	 * @param newEncounter
	 * @param provider
	 * @param parameters
	 * @return
	 */
	protected Encounter processEncounter(String incomingMessageString, Patient p, 
			Date encDate, Encounter newEncounter , Provider provider,
			HashMap<String,Object> parameters){
		
		SocketHL7ListenerService hl7ListService = 
			Context.getService(SocketHL7ListenerService.class);
			
		parameters.put(ChirdlUtilConstants.PARAMETER_NEW_ENCOUNTER_CREATED, true); // CHICA-1157 Use this parameter to determine if TriggerPatientAfterAdvice should run, which creates new patient states
		
		EncounterService es = Context.getEncounterService();
		Encounter enc = null;
		int pid = 0;
		int encid = 0;
		
		try {
			pid = p.getPatientId();
		
			// CHICA-1157 We'll look for encounters starting from the beginning of the day for the location
			// received in the registration message. If we need to allow more than one encounter per day at the given
			// location, we should use the visit number to determine if it is a duplicate, if it is a new visit number,
			// we should be able to assume that it safe to create a new encounter
			// However, visit number is only available at Eskenazi, IUH does not send us visit number
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			
			List<Encounter> encounters = es.getEncounters(p,newEncounter.getLocation(), cal.getTime(), encDate,null,null,null,null,null,false); // CHICA-1151 Add null parameters for Collection<VisitType> and Collection<Visit> CHICA-1157 Add parameter for location
			
			if(!encounters.isEmpty()){ 
				// The patient already has an encounter for the day at this location, treat this message as an update
				enc = encounters.get(0);
				
				// Update the provider as needed
				org.openmrs.Provider openmrsProvider = provider.getProvider(provider);
				EncounterRole encounterRole = es.getEncounterRoleByName(ChirdlUtilConstants.ENCOUNTER_ROLE_ATTENDING_PROVIDER);
				enc.setProvider(encounterRole, openmrsProvider);
				
				// Store the PatientMessage
				hl7ListService.setHl7Message(pid, enc.getEncounterId(), incomingMessageString,
						false, false, this.port);
				
				// From here, updates will occur in the child.processEncounter() method
				parameters.put(ChirdlUtilConstants.PARAMETER_NEW_ENCOUNTER_CREATED, false); // Use this parameter to determine if TriggerPatientAfterAdvice should run, which creates new patient states	
			}else {
				enc = createEncounter(p,newEncounter,provider,parameters);
				if (enc != null && provider != null){
					encid = enc.getEncounterId();
				}else {
					log.warn("Unable to create encount.");
				}
				hl7ListService.setHl7Message(pid, encid, incomingMessageString,
						false, false, this.port);
			}
		} catch (RuntimeException e) {
			log.error("Exception checking encounter date/time.", e);
		} 
		
		
		return enc;
	}
	
	protected void archiveHL7Message(String message){
		AdministrationService adminService = Context.getAdministrationService();
		
		
		String hl7ArchiveDirectory = adminService
		   .getGlobalProperty("sockethl7listener.archiveHL7MessageDirectory");
		try {
			if (hl7ArchiveDirectory != null && !hl7ArchiveDirectory.equals("")){
				if(!(hl7ArchiveDirectory.endsWith("/")||hl7ArchiveDirectory.endsWith("\\")))
				{
					hl7ArchiveDirectory+="/";
				}
				
		        Calendar now = Calendar.getInstance();
		        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HH_mm_ss_SSS");
		        String filename =  formatter.format(now.getTime()) + ".hl7";

				PrintWriter out
				   = new PrintWriter(new BufferedWriter(new FileWriter(hl7ArchiveDirectory + filename)));
				out.write(message);
				out.close();

			}
			
			
		} catch (IOException e) {
			log.error(String.format("Exception writing to directory %s", hl7ArchiveDirectory),e);
		}


		
	}

	public static class Parser {
		
		public static String parseProvider(String matching_input, String attributeName){
			String result = "";
			int index1 = matching_input.indexOf(attributeName);
			int index2 = matching_input.indexOf(":", index1);
			int index3 = matching_input.indexOf(";", index2);
			if (index3 == -1) 
				result = matching_input.substring(index2+1);
			else 
				result = matching_input.substring(index2+1,index3);
			
			return result;
		}
		
		public static  PersonName parseNKName(PersonAttribute NKNameAttr) {
			if(NKNameAttr == null)
			{
				return null;
			}
			String NKnameValue = NKNameAttr.getValue();
			String firstname = "";
			String lastname = "";
			int index1 =NKnameValue.indexOf("|");
			if (index1 != -1){
				firstname = NKnameValue.substring(0,index1);
				lastname = NKnameValue.substring(index1 + 1);
			}else {
				firstname = NKnameValue;
			}
			
			
			PersonName NKName = new PersonName();
			NKName.setFamilyName(lastname);
			NKName.setGivenName(firstname);
				
			return NKName;
			
		}
		
		public PersonName parseProviderName(PersonAttribute provNameAttr) {
		
			String provNameValue = provNameAttr.getValue();
			String firstname = "";
			String lastname = "";
			
			int index1 =provNameValue.indexOf(".");
			if (index1 != -1){
				firstname = provNameValue.substring(0,index1);
				lastname = provNameValue.substring(index1 + 1);
			}else {
				firstname = provNameValue;
			}
			PersonName provName = new PersonName();
			provName.setFamilyName(lastname);
			provName.setGivenName(firstname);
			
			return provName;
	
		}
		
		
	}
	
	private boolean processCWEType(Obs obs,
			 Message message, int orderRep,
			int obxRep, String pIdentifierString, String obsvID,
			String obsValueType)
	{

		Concept answer = this.hl7ObsHandler.getCodedResult(message, orderRep, obxRep, pIdentifierString, obsvID, obsValueType);

		if (answer != null)
		{
			obs.setValueCoded(answer);
			return true;
		}
		return false;
	}

	private boolean processCEType(Obs obs,
			 Message message, int orderRep,
			int obxRep, String pIdentifierString, String obsvID,
			String obsValueType)
	{
		Concept answer = this.hl7ObsHandler.getCodedResult(message, orderRep, obxRep, pIdentifierString, obsvID, obsValueType);

		if (answer != null)
		{
			obs.setValueCoded(answer);
			return true;
		}
		return false;
	}

	private boolean processNMType(Obs obs,
			 Message message, int orderRep,
			int obxRep)
	{
		try
		{
			HL7SocketHandler hl7SocketHandler = this;
			double dVal = hl7SocketHandler.hl7ObsHandler.getNumericResult(message, orderRep, obxRep);
			obs.setValueNumeric(dVal);
			
			// DWE CHICA-635 Get the units from OBX-6 
			// and set it in the concept for this obs
			Concept concept = obs.getConcept();
			if(concept instanceof ConceptNumeric){
				String units = this.hl7ObsHandler.getUnits(message, orderRep, obxRep);
				((ConceptNumeric) concept).setUnits(units);
				obs.setConcept(concept);
			}
			
			return true;
		} catch (APIException e)
		{
			log.error("Exception creating observation for type NM. ", e);
		}
		return false;
	}

	private boolean processTSType(Obs obs,
			 Message message, int orderRep,
			int obxRep)
	{
		try
		{
			Date date = this.hl7ObsHandler.getDateResult(message, orderRep, obxRep);
			if (date != null)
			{
				obs.setValueDatetime(date);
				return true;
			}
		} catch (RuntimeException e)
		{
			log.error("Exception creating observation for type TS.", e);
		}
		return false;
	}

	private boolean processSTType(Obs obs,
			 Message message, int orderRep,
			int obxRep)
	{
		try
		{
			String dataString = this.hl7ObsHandler.getTextResult(message, orderRep,
					obxRep);

			try
			{
				if (dataString != null)
				{
					// see if the string is numeric and store as
					// a number if it is
					Double doubleResult = Double.parseDouble(dataString);

					obs.setValueNumeric(doubleResult);
				}
			} catch (NumberFormatException e)
			{
				obs.setValueText(dataString);
			}
			
			return true;
		} catch (APIException e)
		{
			log.error("Exception creating observation for type ST. ", e);
		}
		return false;
	}
	
	private boolean processTXType(Obs obs,
			 Message message, int orderRep,
			int obxRep)
	{
		return processSTType(obs,message,orderRep,obxRep);
	}
	
	private boolean isNTE(Message message, int orderRep, int obxRep){
		boolean nte = false;
		String value = null;
		//This will only be used for 2.3, however sockethandler is designed for 2.5
		// The methods used for acquiring segments/fields of message are different for 2.3 and 2.5;
		String version = message.getVersion();
		ca.uhn.hl7v2.model.v23.message.ORU_R01 oru23;
		ca.uhn.hl7v2.model.v25.message.ORU_R01 oru25;
		try {
			if (version != null && version.endsWith("2.3")){
				oru23 = (ca.uhn.hl7v2.model.v23.message.ORU_R01) message;
				ca.uhn.hl7v2.model.v23.segment.NTE nteSegment23 =  oru23.getRESPONSE().getORDER_OBSERVATION(orderRep).getNTE();
				if (nteSegment23 !=  null && nteSegment23.getComment()!= null
						&& nteSegment23.getComment().length > 0){
					value = nteSegment23.getComment()[0].getValue();
				}
				
			} else {
				oru25 = (ca.uhn.hl7v2.model.v25.message.ORU_R01) message; 
				NTE nteSegment25 = oru25.getPATIENT_RESULT().getORDER_OBSERVATION(orderRep).getNTE();
				if (nteSegment25 !=  null && nteSegment25.getComment()!= null
						&& nteSegment25.getComment().length > 0){
					value = nteSegment25.getComment()[0].getValue();
				}
			}
			
			if (value != null && !value.equals("")){
					nte = true;
			}
			
		} catch (Exception e) {
			log.error("Exception while extracting NTE segment from hl7",e);
		}
		return nte;
		
	}
	
	
	
	protected void archiveHL7ErrorMessage(String message){
		
		AdministrationService adminService = Context.getAdministrationService();
			
			
			String hl7ArchiveDirectory = adminService
			   .getGlobalProperty("sockethl7listener.archiveHL7ErrorDirectory");
			try {
				if (hl7ArchiveDirectory != null && !hl7ArchiveDirectory.equals("")){
					if(!(hl7ArchiveDirectory.endsWith("/")||hl7ArchiveDirectory.endsWith("\\")))
					{
						hl7ArchiveDirectory+="/";
					}
					
			        Calendar now = Calendar.getInstance();
			        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HH_mm_ss_SSS");
			        String filename =  formatter.format(now.getTime()) + "_error.hl7";

					PrintWriter out
					   = new PrintWriter(new BufferedWriter(new FileWriter(hl7ArchiveDirectory + filename)));
					out.write(message);
					out.close();

				}
				
				
			} catch (IOException e) {
				log.error(String.format("IO Exception archiving an hl7 message to directory: %s ", hl7ArchiveDirectory) ,e);
			}


			
		}
	
	/**
	 * @return the port
	 */
	public Integer getPort() {
		return this.port;
	}



	/**
	 * @param port the port to set
	 */
	public void setPort(Integer port) {
		this.port = port;
	}
	
	 
	/**
	 * Prepares message and sends message on designated port
	 * @param host
	 * @param port
	 * @param message
	 * @throws IOException
	 */
	public Date sendMessage(Encounter encounter , String host, Integer port, 
			String message, Integer timeoutSec) throws IOException{
		if (timeoutSec == null || timeoutSec == 0)timeoutSec = 5;
		
		String Hl7StartMessage = "\u000b";
		String Hl7EndMessage = "\u001c";
		SocketHL7ListenerService hl7ListService = Context.getService(SocketHL7ListenerService.class);
		
		 hl7ListService.saveMessageToDatabase(encounter, message, null, port, host);
		if (this.os != null){
			this.os.write( Hl7StartMessage.getBytes() );
			this.os.write(message.getBytes());
	        this.os.write( Hl7EndMessage.getBytes() );
	        this.os.write(13);
	        this.os.flush();
		}
		this.socket.setSoTimeout(timeoutSec * 1000);
		String result = readAck();
		log.error("Read ACK: {}" , result);
		Date ackDate = null;
		if (result != null){ 
			ackDate = new Date();
			hl7ListService.saveMessageToDatabase(encounter, message, ackDate, port, host);
		}

        return ackDate;
	}
	
	/**
	 * Prepares message and sends message on designated port
	 * @param host
	 * @param port
	 * @param message
	 * @throws IOException
	 */
	public Date sendMessage(Encounter encounter , String host, Integer port, 
			String message, Integer timeoutSec, boolean readAck) throws IOException{
		if (timeoutSec == null || timeoutSec == 0)timeoutSec = 5;
		
		String hl7StartMessage = "\u000b";
		String hl7EndMessage = "\u001c";
		SocketHL7ListenerService hl7ListService = Context.getService(SocketHL7ListenerService.class);
		
		 hl7ListService.saveMessageToDatabase(encounter, message, null, port, host);
		if (this.os != null){
			this.os.write( hl7StartMessage.getBytes() );
			this.os.write(message.getBytes());
	        this.os.write( hl7EndMessage.getBytes() );
	        this.os.write(13);
	        this.os.flush();
		}
		this.socket.setSoTimeout(timeoutSec * 1000);
		String result = null;
		if (readAck) {
			
			result = readAck();
			log.error("Read ACK: {} ", result);
		}
		Date ackDate = null;
		if (result != null){ 
			ackDate = new Date();
		}
		hl7ListService.saveMessageToDatabase(encounter, message, ackDate, port, host);

        return ackDate;
	}
	
	public HL7Outbound sendMessage(HL7Outbound hl7Out, Integer timeoutSec) throws IOException{
		if (timeoutSec == null || timeoutSec == 0)timeoutSec = 5;
		String hl7StartMessage = "\u000b";
		String hl7EndMessage = "\u001c";
		SocketHL7ListenerService hl7ListService = Context.getService(SocketHL7ListenerService.class);
		
		 hl7Out = hl7ListService.saveMessageToDatabase(hl7Out);
		 if (this.os != null){
			this.os.write( hl7StartMessage.getBytes() );
			this.os.write( hl7Out.getHl7Message().getBytes());
	        this.os.write( hl7EndMessage.getBytes() );
	        this.os.write(13);
	        this.os.flush();
		}
		
		this.socket.setSoTimeout(timeoutSec * 1000);
		try {
			String result = readAck();
			log.error("Read ACK: {}", result);
			Date ackDate = null;
			if (result != null){ 
				ackDate = new Date();
				hl7Out.setAckReceived(ackDate);
				hl7ListService.saveMessageToDatabase(hl7Out);
				}
		} catch (Exception e) {
			log.error("Error during readAck", e);
		}

        return hl7Out;
	}
	
	
	public void openSocket(String host, Integer port) throws IOException{
        try {
        	this.socket = new Socket(host, port);
			this.socket.setSoLinger(true, 10000);
			
			this.os = this.socket.getOutputStream();
			this.is = this.socket.getInputStream();
			
		} catch (Exception e) {
			log.error("Open socket failed for host= " + host + " port= " + port, e);
		}
    } 
	
	 public void closeSocket() {
	        try {
	            Socket sckt = this.socket;
	            this.socket = null;
	            this.os.close();
	            this.is.close();
	           
	            if (sckt != null)
	                sckt.close();
	           
	        }
	        catch (Exception e) {
	            
	        }
	    }
	 
	 private String readAck() throws IOException
		{
			StringBuffer stringbuffer = new StringBuffer();
			int i = 0;
			do {
				i = this.is.read();
				if (i == -1)
					return null;
	            
				stringbuffer.append((char) i);
			}
			while (i != 28);        
			return stringbuffer.toString();
		}

	/**
	 * @return the socket
	 */
	public Socket getSocket() {
		return this.socket;
	}

	/**
	 * @param socket the socket to set
	 */
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
}




