package org.openmrs.module.sockethl7listener.service;

import org.openmrs.Encounter;


/**
 * Defines services used by this module
 *  
 * @author Tammy Dugan
 *
 */
public interface SocketHL7ListenerService
{
	public void saveMessageToDatabase(Encounter enc, String encodedMessage);

	public void setHl7Message(int pid, int encounter_id,  String message, boolean dup_string,
			boolean dup_enc, Integer port);

	public String getNPI(String firstName, String lastName);
	
	public boolean checkMD5(String incoming, Integer port);
	
	public void messageProcessed(Encounter encounter);
	
	public String getFaxNumber(String firstName, String lastName);
}