/**
 * 
 */
package org.openmrs.module.sockethl7listener;

import ca.uhn.hl7v2.model.Message;

/**
 * @author tmdugan
 *
 */
public interface HL7Filter
{
	public boolean ignoreMessage(HL7EncounterHandler hl7EncounterHandler,
			Message message,String incomingMessageString);
}
