/*package org.openmrs.module.sockethl7listener;

import org.openmrs.api.EncounterService;
import org.openmrs.api.ObsService;
import org.openmrs.api.PatientService;
import org.openmrs.api.PersonService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;



public class HL7AlertService {
	private EncounterService es;
	private PatientService patientService;
	private PersonService personService;
	private ObsService os;
	private UserService us;
	
		
	public HL7AlertService(){
		es = Context.getEncounterService();
		patientService = Context.getPatientService();
		personService = Context.getPersonService();
		os = Context.getObsService();
		us = Context.getUserService();
		
	}
	
	public void createPID (){
		String textMSH = "";
		String header = "MSH|^~\\&|";
		
		//get the encounter id from nbs_alert table
		
		//facility 
		//application
		//our facility
		//date of message - get current time
		String version = "2.5";
		
	}
	public void createMSH (){
		
	}
	public void createNK1 (){
	
	}
	public void createPV1 (){
	
	}
	public void createOBR (){
	
	
	}
	public void createOBX (){
	
	}

}*/
