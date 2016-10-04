/**
 * 
 */
package org.openmrs.module.sockethl7listener;

import java.util.Date;

import ca.uhn.hl7v2.model.Message;

/**
 * @author tmdugan
 *
 */
public interface HL7EncounterHandler
{
	public Provider getProvider(Message message);
	
	public Date getEncounterDate(Message message);
	
	public String getVisitNumber(Message message); // DWE CHICA-633
	
	public String getLocationDescription(Message message); // DWE CHICA-751
}
