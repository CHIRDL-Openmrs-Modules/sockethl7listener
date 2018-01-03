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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.apache.log4j.Logger;
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
import org.openmrs.hl7.HL7Constants;
import org.openmrs.hl7.HL7InQueue;
import org.openmrs.hl7.HL7Service;
import org.openmrs.hl7.HL7Source;
import org.openmrs.module.chirdlutil.util.ChirdlUtilConstants;
import org.openmrs.module.chirdlutil.util.DateUtil;
import org.openmrs.module.sockethl7listener.hibernateBeans.HL7Outbound;
import org.openmrs.module.sockethl7listener.service.SocketHL7ListenerService;
import org.openmrs.module.sockethl7listener.util.Util;
import org.openmrs.util.PrivilegeConstants;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.app.Application;
import ca.uhn.hl7v2.app.ApplicationException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
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
	private static final String DATE_FORMAT_YYYY_MM_DD_HH_MM = "yyyyMMddHHmm";
	private static final String DATE_FORMAT_YYYY_MM_DD_HH_MM_SS = "yyyyMMddHHmmss";
	private static final String DATE_FORMAT_YYYY_MM_DD = "yyyyMMdd";
	protected static final Logger logger = Logger.getLogger("SocketHandlerLogger");
	private static final Logger conceptNotFoundLogger = Logger.getLogger("ConceptNotFoundLogger");
		
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
		
		if (port == null){
			port = 0;
		}
		if (host == null){
			host = "localhost";
		}
		
	}
	
	public HL7SocketHandler(ca.uhn.hl7v2.parser.Parser parser,
			PatientHandler patientHandler, HL7ObsHandler hl7ObsHandler,
			HL7EncounterHandler hl7EncounterHandler,
			HL7PatientHandler hl7PatientHandler,
			ArrayList<HL7Filter> filters)
	{
		
		this.patientHandler = patientHandler;
		this.parser = parser;
		this.hl7ObsHandler = hl7ObsHandler;
		this.hl7EncounterHandler = hl7EncounterHandler;
		this.hl7PatientHandler = hl7PatientHandler;
		this.filters = filters;
	}
	
	/**
	 * Returns true if the message is not null and is an instance of ADT_A01 (which A04 and A08 are since hapi uses the same message structure for all A0x messages)
	 * 
	 * @returns true
	 */
	public boolean canProcess(Message message) {
		return message != null && message instanceof ca.uhn.hl7v2.model.v25.message.ADT_A01;
	}

	protected Message processMessage(Message message, HashMap<String, Object> parameters) {
		Message response = null;
		boolean error = false;
		
		try {
			HL7Service hl7Service = Context.getHL7Service();
			AdministrationService adminService = Context.getAdministrationService();
			Context.openSession();
			String incomingMessageString = "";
			try 
			{
				incomingMessageString = this.parser.encode(message);
			} catch (HL7Exception e2) 
			{
				e2.printStackTrace();
			}
						
			if (!(message instanceof ORU_R01) && !(message instanceof ADT_A01)) 
			{
				String messageType = "";
				
				if (message.getParent() != null) {
					messageType = message.getParent().getName();
				}
				throw new ApplicationException(

				"Invalid message type (" + messageType
				        + ") sent to HL7 Socket handler. Only ORU_R01 and ADT_A01 valid currently. ");
			}
			if (logger.isDebugEnabled())
				logger.debug("Depositing HL7 ORU_R01 message in HL7 queue.");
			
			try {
				
				Context.authenticate(adminService
				.getGlobalProperty("scheduler.username"), adminService
				        .getGlobalProperty("scheduler.password"));
				Context.addProxyPrivilege(PrivilegeConstants.PRIV_ADD_HL7_IN_QUEUE); // CHICA-1151 replaced HL7Constants.PRIV_ADD_HL7_IN_QUEUE with PrivilegeConstants.PRIV_ADD_HL7_IN_QUEUE
				if (!Context.hasPrivilege(PrivilegeConstants.PRIV_ADD_HL7_IN_QUEUE)) {
					logger.error("You do not have HL7 add privilege!!");
					System.exit(0);
				}
				
				HL7Source hl7Source = new HL7Source();
				
				if (hl7Service.getHL7SourceByName(port.toString()) == null) {
					hl7Source.setName(String.valueOf(port));
					hl7Source.setDescription("Port for hl7 message.");
					hl7Service.saveHL7Source(hl7Source);
				} else {
					hl7Source = hl7Service.getHL7SourceByName(port.toString());
				}
				
				HL7InQueue hl7inQ = new HL7InQueue();
				hl7inQ.setHL7Source(hl7Source);
				hl7inQ.setHL7Data(incomingMessageString);
				//MessageState 0=pending, 1=processing, 2=processed, 3=error
				hl7inQ.setMessageState(1);
				HL7InQueue savedHl7 = hl7Service.saveHL7InQueue(hl7inQ);
				
				archiveHL7Message(incomingMessageString);
				
				boolean ignoreMessage = false;
				
				if (this.filters != null) {
					for (HL7Filter filter : filters) {
						if (filter.ignoreMessage(hl7EncounterHandler, 
						message, incomingMessageString)) {
							ignoreMessage = true;
							break;
						}
					}
				}
				
				if (!ignoreMessage) {
					error = processMessageSegments(message, incomingMessageString, parameters);
				}
				try {
					MSH msh = HL7ObsHandler25.getMSH(message);
					response = Util.makeACK(msh, error, null, null);
				}catch (IOException e) {
					logger.error("Error generating message id for ACK message" + e.getMessage());
				}catch (HL7Exception e){
					logger.error("Error during ACK message construction",e);
				}
				
				Context.clearSession();
				
				savedHl7.setMessageState(2);
				Context.getHL7Service().saveHL7InQueue(savedHl7);
				
			}catch (ContextAuthenticationException e) {
				logger.error("Context Authentication exception: ", e);
				Context.closeSession();
				System.exit(0);
			}catch (ClassCastException e) {
				logger.error("Error casting to " + message.getClass().getName() + " ",
				 e);
				throw new ApplicationException("Invalid message type for handler");
			}catch (HL7Exception e) {
				logger.error("Error while processing hl7 message", e);
				throw new ApplicationException(e);
			}finally {
				Context.closeSession();
			}
		}
		catch (Exception e) {
			logger.error("", e);
		}
		finally {
			if (response == null) {
				try {
					error = true;
					MSH msh = HL7ObsHandler25.getMSH(message);
					response = Util.makeACK(msh, error, null, null);
				}
				catch (Exception e) {
					logger.error("Could not send acknowledgement", e);
				}
			}
		}
		
		return response;
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
			logger.error("Could not create a provider or find an existing provider for: firstname=" 
					+ provider.getFirstName() + " lastname=" + provider.getLastName() + " id=" 
					+ provider.getEhrProviderId()  );
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
		
		if (encounter == null) return null;
		
		int reps = hl7ObsHandler.getReps(message);// number of obs 

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

	private boolean processMessageSegments(Message message,String incomingMessageString,HashMap<String,Object> parameters) throws HL7Exception {
		SocketHL7ListenerService hl7ListService = Context.getService(SocketHL7ListenerService.class);
		
		EncounterService encounterService = Context.getEncounterService();
		AdministrationService adminService = Context.getAdministrationService();
		// validate message
		Date starttime = new Date();
		validate(message);
		boolean error = false;

		MSH msh = HL7ObsHandler25.getMSH(message);
		parameters.put("sendingApplication", msh.getSendingApplication().getNamespaceID().getValue());
		parameters.put("sendingFacility", msh.getSendingFacility().getNamespaceID().getValue());
		
		// Obtain message control id (unique ID for message from sending
		// application)
		String messageControlId = msh.getMessageControlID().getValue();
		if (logger.isDebugEnabled())
			logger.debug("Found HL7 message in inbound queue with control id = "
					+ messageControlId);
		
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
				
				String locationString = hl7EncounterHandler.getLocation(message); // CHICA-982 Get location from PV1 instead of ZLR segment
				Location encounterLocation = setLocation(locationString);
				Date encounterDate = hl7EncounterHandler.getEncounterDate(message);
				Patient hl7Patient = patientHandler.setPatientFromHL7(message,encounterDate,encounterLocation,hl7PatientHandler);
				
				// Extract provider information for provider observations.
				Provider provider = hl7EncounterHandler.getProvider(message);

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
			logger.info("MESSAGE PROCESS TIME: " + duration + " sec");
			
		} catch (RuntimeException e) {
			//Do not stop application. Start processing next hl7 message.
			logger.error("RuntimeException processing ORU_RO1", e);
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
						hl7Patient,encounterDate);
			}
			
			
		} catch (RuntimeException e) {
			logger.error("Exception creating or updating patient. " + e.getMessage() 
					+ " pid = " +  resultPatient.getPatientId());
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
		} catch (APIException e)
		{
			logger.error(" createEncounter() or createUser() api failed. ", e);
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

		boolean okToCreateObs = true;
		String pIdentifierString = resultPatient.getPatientIdentifier().getIdentifier();
		Obs obs = new Obs();
		
		// Get obsv type
		String obsValueType = hl7ObsHandler.getObsValueType(message, orderRep,obxRep);
		if (obsValueType == null) {
			okToCreateObs = false;
		}
		
		// set Observation date/time
		//Modified to check for instance of ORU_RO1
		//Reason: Previously, the only messages that had OBX
		//were ORU_R01.  However, some projects have ADT
		//messages with observations. The method isNTE() assumes
		//messages as ORU, so check first. 
		
		//MES CHICA-358 Use OBR datetime if OBX is null
		Date obsDateTime = hl7ObsHandler.getObsDateTime(message, orderRep, obxRep);
		
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
							logger.error("Invalid date / time" + datetime  );
						}
					}
				}
			}
		} catch (Exception e){
			logger.error("Exception getting obs datetime from OBX/OBR. " 
					+ org.openmrs.module.chirdlutil.util.Util.getStackTrace(e));
		}
		
		obs.setObsDatetime(obsDateTime);

		// set Location
		Location loc = new Location();
		String sendingFacility = hl7ObsHandler.getSendingFacility(message);
		loc.setName(sendingFacility);
		try {
			if (existingLoc != null) {
				obs.setLocation(existingLoc);
			} else {
				obs.setLocation(loc);
			}
		} catch (APIException e) {
			logger.error("Unable to set location.");
		}
		
		obs.setPerson(resultPatient);
		
		// Set the encounter based on the encounter id
		obs.setEncounter(enc);
		
		String conceptName = null;

		// Get observation question/concept from field 3 Always CE type
		// Assumption that first field is always a string representing an
		// integer

		String stConceptId = hl7ObsHandler.getConceptId(message, orderRep, obxRep);
		int conceptId = 0;

		// if the conceptId isn't there, lookup the code by name
		conceptName = hl7ObsHandler.getConceptName(message, orderRep, obxRep);
		try
		{
			conceptId = Integer.parseInt(stConceptId);

		} catch (NumberFormatException e)
		{
			if (logger.isDebugEnabled())
				logger
						.warn("PARSE WARNING: Unable to parse integer from observation id string - Invalid value in OBX ");
		}
		
		Concept concept = null;
		
		if(saveToDatabase)
		{
			concept = Util.lookupConcept(conceptId, conceptName);
			
			if(concept == null)
			{
				okToCreateObs = false;
			}
		}else
		{
			// DWE CHICA-635
			if(obsValueType.equals(HL7Constants.HL7_NUMERIC)){
				concept = new ConceptNumeric();
			}else{
				concept = new Concept();
			}
			concept.setConceptId(conceptId);
			ConceptName name = new ConceptName();
			name.setName(conceptName);
			name.setLocale(new Locale("en_US"));
			concept.addName(name);
		}
		
		obs.setConcept(concept);

		// Meeting on 3/20/07 - only one set of data in field 5. There will be
		// no "~"
		// Varies value = values[0];

		//set the result by type
		if (okToCreateObs)
		{
			if (obsValueType != null)
			{
				if (obsValueType.equals("CWE"))
				{
					okToCreateObs = processCWEType(obs, message, orderRep,
							obxRep, pIdentifierString, stConceptId,
							obsValueType);
				} else if (obsValueType.equals("CE"))
				{
					okToCreateObs = processCEType(obs, message, orderRep,
							obxRep, pIdentifierString, stConceptId,
							obsValueType);
				} else if (obsValueType.equals("NM"))
				{
					okToCreateObs = processNMType(obs, message, orderRep,
							obxRep);
				} else if (obsValueType.equals("TS"))
				{
					okToCreateObs = processTSType(obs, message, orderRep,
							obxRep);
				} else if (obsValueType.equals("ST"))
				{
					okToCreateObs = processSTType(obs, message, orderRep,
							obxRep);
				} else if (obsValueType.equals("TX"))
				{
					okToCreateObs = processTXType(obs, message, orderRep,
							obxRep);
				}
			}
			
			//create the obs
			if(okToCreateObs&&saveToDatabase)
			{
				os.saveObs(obs,null);
				enc.addObs(obs);
			}
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
			sendingLocString = hl7ObsHandler.getSendingFacility(message);
			existingLocation = locationService.getLocation(sendingLocString);
			if (existingLocation != null) {
				return existingLocation;
			} 
				newLocation.setName(sendingLocString);
				locationService.saveLocation(newLocation);
				return newLocation;
		} catch (APIException e) {
			logger.warn("Error creating the new location. Message: "
					+ e.getMessage());

		} catch (RuntimeException e) {
			logger
			.warn("Unable to parse the sending facility from MSH. Message:"
					+ e.getMessage());
		}
		
		return null;
	}
	
	protected Patient createPatient(Patient p){
		
		PatientService patientService = Context.getPatientService();
		Patient resultPatient = patientService.savePatient(p);
			
		return resultPatient;
	}
	
	protected Patient updatePatient(Patient mp,
			Patient hl7Patient,Date encounterDate){
	
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
			logger.error(e.getMessage(),e);
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
		AdministrationService adminService = Context.getAdministrationService();	
		String window = adminService
		   .getGlobalProperty("sockethl7listener.encounterDateTimeWindow");
		String ignoreDuplicateEncounter = adminService
		   .getGlobalProperty("sockethl7listener.ignoreDuplicateEncounter");
		
		EncounterService es = Context.getEncounterService();
		Encounter enc = null;
		int pid = 0;
		boolean isDuplicateDateTime = false;
		int encid = 0;
		
		try {
			pid = p.getPatientId();
		
			//Check for any encounter with the same encounter date/time 
			//or within a specified window of this encounter date/time.
			Integer timeWindow = 0;
			
			Calendar cal = Calendar.getInstance();
			if (window != null) {
				try {
					timeWindow = Integer.valueOf(window);
				} catch (NumberFormatException e) {
					//global property string is not an Integer 
					//set time window = 0
				}
			}
			
			cal.setTime(encDate);
			cal.add(Calendar.MINUTE, -timeWindow);
			Date fromDate = cal.getTime();

			List<Encounter> encounters = es.getEncounters(p,null, fromDate, encDate,null,null,null,null,null,false); // CHICA-1151 Add null parameters for Collection<VisitType> and Collection<Visit>
					
			Iterator <Encounter> it = encounters.iterator();
			if (it.hasNext()){
				enc = encounters.iterator().next();
				isDuplicateDateTime = true;
				encid = enc.getEncounterId();
				if (ignoreDuplicateEncounter  != null 
						&& (ignoreDuplicateEncounter.equalsIgnoreCase("true")
								|| ignoreDuplicateEncounter.equalsIgnoreCase("0") 
								||  ignoreDuplicateEncounter.equalsIgnoreCase("yes"))){
					hl7ListService.setHl7Message(pid, encid, incomingMessageString,
							false, isDuplicateDateTime, this.port);
					logger.error("Encounter occurred within " + window 
							+ " minutes of a previous encounter (" + encid + ") for patient " + pid);
					return null;
				}
			}else {
				enc = createEncounter(p,newEncounter,provider,parameters);
				if (enc != null && provider != null){
					encid = enc.getEncounterId();
				}else {
					logger.warn("Encounter not created or provider is null ");
				}
						
			}
			hl7ListService.setHl7Message(pid, encid, incomingMessageString,
					false, isDuplicateDateTime, this.port);
			
			
		} catch (RuntimeException e) {
			logger.error("Exception when checking encounter date/time. ",e);
		} 
		
		
		return enc;
	}
	
	private void archiveHL7Message(String message){
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
			e.printStackTrace();
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
		
			String ProvNameValue = provNameAttr.getValue();
			String firstname = "";
			String lastname = "";
			
			int index1 =ProvNameValue.indexOf(".");
			if (index1 != -1){
				firstname = ProvNameValue.substring(0,index1);
				lastname = ProvNameValue.substring(index1 + 1);
			}else {
				firstname = ProvNameValue;
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

		Concept answer = hl7ObsHandler.getCodedResult(message, orderRep, obxRep,
				logger, pIdentifierString, obsvID, obsValueType,
				conceptNotFoundLogger);

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
		Concept answer = hl7ObsHandler.getCodedResult(message, orderRep, obxRep,
				logger, pIdentifierString, obsvID, obsValueType,
				conceptNotFoundLogger);

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
			double dVal = hl7ObsHandler
					.getNumericResult(message, orderRep, obxRep);
			obs.setValueNumeric(dVal);
			
			// DWE CHICA-635 Get the units from OBX-6 
			// and set it in the concept for this obs
			Concept concept = obs.getConcept();
			if(concept instanceof ConceptNumeric){
				String units = hl7ObsHandler.getUnits(message, orderRep, obxRep);
				((ConceptNumeric) concept).setUnits(units);
				obs.setConcept(concept);
			}
			
			return true;
		} catch (APIException e)
		{
			logger.error("Exception creating observation for type NM. ", e);
		}
		return false;
	}

	private boolean processTSType(Obs obs,
			 Message message, int orderRep,
			int obxRep)
	{
		try
		{
			Date date = hl7ObsHandler.getDateResult(message, orderRep, obxRep);
			if (date != null)
			{
				obs.setValueDatetime(date);
				return true;
			}
		} catch (APIException e)
		{
			logger.error("Exception creating observation for type TS. " +  e.getMessage());
		}
		return false;
	}

	private boolean processSTType(Obs obs,
			 Message message, int orderRep,
			int obxRep)
	{
		try
		{
			String dataString = hl7ObsHandler.getTextResult(message, orderRep,
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
			logger.error("Exception creating observation for type ST. ", e);
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
			logger.error("Exception while extracting NTE segment from hl7",e);
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
				e.printStackTrace();
			}


			
		}
	
	/**
	 * @return the port
	 */
	public Integer getPort() {
		return port;
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
		if (os != null){
			os.write( Hl7StartMessage.getBytes() );
			os.write(message.getBytes());
	        os.write( Hl7EndMessage.getBytes() );
	        os.write(13);
	        os.flush();
		}
		socket.setSoTimeout(timeoutSec * 1000);
		String result = readAck();
		logger.error("Read ACK: " + result);
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
		
		String Hl7StartMessage = "\u000b";
		String Hl7EndMessage = "\u001c";
		SocketHL7ListenerService hl7ListService = Context.getService(SocketHL7ListenerService.class);
		
		 hl7ListService.saveMessageToDatabase(encounter, message, null, port, host);
		if (os != null){
			os.write( Hl7StartMessage.getBytes() );
			os.write(message.getBytes());
	        os.write( Hl7EndMessage.getBytes() );
	        os.write(13);
	        os.flush();
		}
		socket.setSoTimeout(timeoutSec * 1000);
		String result = null;
		if (readAck) {
			
			result = readAck();
			logger.info("Read ACK: " + result);
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
		String Hl7StartMessage = "\u000b";
		String Hl7EndMessage = "\u001c";
		SocketHL7ListenerService hl7ListService = Context.getService(SocketHL7ListenerService.class);
		
		 hl7Out = hl7ListService.saveMessageToDatabase(hl7Out);
		 if (os != null){
			os.write( Hl7StartMessage.getBytes() );
			os.write( hl7Out.getHl7Message().getBytes());
	        os.write( Hl7EndMessage.getBytes() );
	        os.write(13);
	        os.flush();
		}
		
		socket.setSoTimeout(timeoutSec * 1000);
		try {
			String result = readAck();
			logger.error("Read ACK: " + result);
			Date ackDate = null;
			if (result != null){ 
				ackDate = new Date();
				hl7Out.setAckReceived(ackDate);
				hl7ListService.saveMessageToDatabase(hl7Out);
				}
		} catch (Exception e) {
			logger.error("Error during readAck", e);
		}

        return hl7Out;
	}
	
	
	public void openSocket(String host, Integer port) throws IOException{
        try {
        	socket = new Socket(host, port);
			socket.setSoLinger(true, 10000);
			
			os = socket.getOutputStream();
			is = socket.getInputStream();
			
		} catch (Exception e) {
			logger.error("Open socket failed: " + e.getMessage());
		}
    } 
	
	 public void closeSocket() {
	        try {
	            Socket sckt = socket;
	            socket = null;
	            os.close();
	            is.close();
	           
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
				i = is.read();
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
		return socket;
	}

	/**
	 * @param socket the socket to set
	 */
	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	
}




