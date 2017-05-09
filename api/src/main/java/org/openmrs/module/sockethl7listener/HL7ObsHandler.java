/**
 * 
 */
package org.openmrs.module.sockethl7listener;

import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Patient;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;

/**
 * @author tmdugan
 *
 */
public interface HL7ObsHandler
{
	public String getSendingFacility(Message message);

	public Date getDateStarted(Message message);

	public Date getDateStopped(Message message);
	
	public String getObsValueType(Message message, int orderRep,int obxRep);
	
	public Date getObsDateTime(Message message, int orderRep, int obxRep);
	
	public String getConceptId(Message message,int orderRep,int obxRep);
	
	public String getConceptName(Message message,int orderRep,int obxRep);
	
	public String getTextResult(Message message, int orderRep, int obxRep);
	
	public Date getDateResult(Message message, int orderRep, int obxRep);
	
	public Double getNumericResult(Message message, int orderRep, int obxRep);
	
	public Concept getCodedResult(Message message, int orderRep, int obxRep,
			Logger logger,String pIdentifierString,String obsvID,
			String obsValueType,Logger conceptNotFoundLogger);
	
	public ArrayList<Obs> getObs(Message message, Patient patient) throws HL7Exception;

	public int getReps(Message message);
	
	/**
	 * DWE CHICA-635
	 * 
	 * Get the units from OBX-6
	 * 
	 * @param message
	 * @param orderRep
	 * @param obxRep
	 * @return
	 */
	public String getUnits(Message message, int orderRep, int obxRep);
}
