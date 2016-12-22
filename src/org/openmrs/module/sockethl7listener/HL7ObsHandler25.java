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
import org.openmrs.module.sockethl7listener.util.Util;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v25.datatype.CE;
import ca.uhn.hl7v2.model.v25.datatype.CWE;
import ca.uhn.hl7v2.model.v25.datatype.ID;
import ca.uhn.hl7v2.model.v25.datatype.NM;
import ca.uhn.hl7v2.model.v25.datatype.ST;
import ca.uhn.hl7v2.model.v25.datatype.TS;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.segment.OBX;
import org.openmrs.hl7.HL7Util;

/**
 * @author tmdugan
 * 
 */
public class HL7ObsHandler25 implements HL7ObsHandler
{
	private static final Logger logger = Logger.getLogger("SocketHandlerLogger");
	public static MSH getMSH(Message message)
	{
		if (message instanceof ORU_R01)
		{
			return getMSH((ORU_R01) message);
		}
		if (message instanceof ADT_A01)
		{
			return getMSH((ADT_A01) message);
		}
		return null;
	}

	private static MSH getMSH(ORU_R01 oru)
	{
		return oru.getMSH();
	}

	private static MSH getMSH(ADT_A01 adt)
	{
		return adt.getMSH();
	}

	public static OBX getOBX(Message message, int orderRe, int obRep)
	{
		if (message instanceof ORU_R01)
		{
			return getOBX((ORU_R01) message, orderRe, obRep);
		}
		else if ((message instanceof ADT_A01)) // DWE CHICA-635 Handle instance of ADT_A01
		{
			return getOBX((ADT_A01) message, orderRe, obRep);
		}
		return null;
	}

	private static OBX getOBX(ORU_R01 oru, int orderRep, int obRep)
	{

		OBX obx = null;
		try
		{
			obx = oru.getPATIENT_RESULT().getORDER_OBSERVATION(orderRep)
					.getOBSERVATION(obRep).getOBX();
		} catch (Exception e)
		{

			logger.error("Exception" ,e);
		}

		return obx;
	}
	
	/**
	 * DWE CHICA-635 
	 * Get OBX from ADT_A01 message
	 * @param adt
	 * @param orderRep
	 * @param obRep
	 * @return
	 */
	private static OBX getOBX(ADT_A01 adt, int orderRep, int obRep)
	{
		OBX obx = null;
		try
		{
			obx = adt.getOBX(obRep);
		}
		catch(Exception e)
		{
			logger.error("Exception" ,e);
		}
		
		return obx;
	}

	public static OBR getOBR(Message message, int orderRep)
	{
		if (message instanceof ORU_R01)
		{
			return getOBR((ORU_R01) message, orderRep);
		}

		return null;
	}

	private static OBR getOBR(ORU_R01 oru, int orderRep)
	{

		OBR obr = null;
		try
		{
			obr = oru.getPATIENT_RESULT().getORDER_OBSERVATION(orderRep)
					.getOBR();
		} catch (Exception e)
		{
			logger.error("Exception" ,e);
		}

		return obr;
	}

	
	/**
	 * @param ts
	 * @return Date that was extraced from the TS data type
	 */
	public static Date TranslateDate(TS ts)
	{
		Date datetime = null;
		String timeString = null;
		try {
			if (ts != null && ts.getTime() != null ){
				timeString = ts.getTime().getValue();
				if (timeString != null) {
					datetime = HL7Util.parseHL7Timestamp(timeString);
				}
			}	
		}  catch (HL7Exception e) {
			logger.error("Error converting TS timestamp to Date due to parsing error. Time string: " + timeString);
		}
		
		return datetime;
		
		
	}

	public String getSendingFacility(Message message)
	{
		if (!(message instanceof ORU_R01))
		{
			return null;
		}

		MSH msh = getMSH((ORU_R01) message);
		return msh.getSendingFacility().getNamespaceID().getValue();
	}

	public Date getDateStarted(Message message)
	{
		if (!(message instanceof ORU_R01))
		{
			return null;
		}
		// OBR segment --Observation start time
		TS tsObsvStartDateTime = getOBR((ORU_R01) message, 0)
				.getObservationDateTime();
		if (tsObsvStartDateTime.getTime().getValue() == null)
		{
			tsObsvStartDateTime = getMSH((ORU_R01) message)
					.getDateTimeOfMessage();
		}
		Date sdt = TranslateDate(tsObsvStartDateTime);
		return sdt;
	}

	public Date getDateStopped(Message message)
	{
		if (!(message instanceof ORU_R01))
		{
			return null;
		}

		// OBR Segment Observation stop time - usually not present
		Date edt = null;
		TS tsObsvEndDateTime = getOBR((ORU_R01) message, 0)
				.getObservationEndDateTime();
		if (tsObsvEndDateTime.getTime().getValue() != null)
		{
			edt = TranslateDate(tsObsvEndDateTime);
		}
		return edt;
	}

	public String getObsValueType(Message message, int orderRep, int obxRep)
	{
		ID valueType = getOBX(message, orderRep, obxRep).getValueType();
		if(valueType != null){
			return valueType.toString();
		}
		return null;
	}

	public Date getObsDateTime(Message message, int orderRep, int obxRep)
	{
		TS tsObsDateTime = getOBX(message, orderRep, obxRep)
				.getDateTimeOfTheObservation();
		Date obsDateTime = TranslateDate(tsObsDateTime);
		return obsDateTime;
	}

	public String getConceptId(Message message, int orderRep, int obxRep)
	{
		CE ceObsIdentifier = getOBX(message, orderRep, obxRep)
				.getObservationIdentifier();
		return ceObsIdentifier.getIdentifier().toString();
	}

	public String getConceptName(Message message, int orderRep, int obxRep)
	{
		CE ceObsIdentifier = getOBX(message, orderRep, obxRep)
				.getObservationIdentifier();
		return ceObsIdentifier.getText().toString();
	}

	public String getTextResult(Message message, int orderRep, int obxRep)
	{
		Varies[] values = getOBX(message, orderRep, obxRep)
				.getObservationValue();
		Varies value = null;
		if (values.length > 0)
		{
			value = values[0];

			ST data = (ST) value.getData();
			String dataString = data.getValue();
			return dataString;
		}
		return null;
	}

	public Date getDateResult(Message message, int orderRep, int obxRep)
	{
		Varies[] values = getOBX(message, orderRep, obxRep)
				.getObservationValue();
		Varies value = null;
		if (values.length > 0)
		{

			value = values[0];
			TS ts = (TS) value.getData();
			Date date = TranslateDate(ts);
			return date;
		}
		return null;
	}

	public Double getNumericResult(Message message, int orderRep, int obxRep)
	{
		double dVal = 0;
		Varies[] values = getOBX(message, orderRep, obxRep)
				.getObservationValue();
		Varies value = null;
		if (values.length > 0)
		{
			value = values[0];
			String nmvalue = ((NM) value.getData()).getValue();

			if (nmvalue != null)
			{
				try
				{
					dVal = Double.parseDouble(nmvalue);
				} catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		}
		return dVal;
	}

	protected Concept processCWEType(Varies value, Logger logger,
			String pIdentifierString, String conceptQuestionId,
			Logger conceptNotFoundLogger)
	{
		Integer conceptId = null;
		String stConceptId = ((CWE) value.getData()).getIdentifier().toString();
		String conceptShortName = ((CWE) value.getData()).getText().toString();
		String nameOfCodingSystem = ((CWE) value.getData())
				.getNameOfCodingSystem().toString();
		String conceptName = ((CWE) value.getData()).getAlternateIdentifier()
				.toString();
		String conceptLongName = ((CWE) value.getData()).getAlternateText()
				.toString();

		try
		{
			conceptId = Integer.parseInt(stConceptId);
		} catch (NumberFormatException ne)
		{
			logger
					.warn("PARSE WARNING: Unable to parse integer from CWE observation value id string - Invalid value in OBX  = "
							+ stConceptId
							+ " for patient MRN "
							+ pIdentifierString
							+ " \n concept question id : "
							+ conceptQuestionId
							+ " concept name: "
							+ conceptName
							+ "long name = "
							+ conceptLongName
							+ " Check coded value.");
		}

		// Success conversion to int
		if (conceptId != null)
		{
			try
			{
				Concept answer = Util.lookupConcept(conceptId, conceptName);
				if (answer == null)
				{
					conceptNotFoundLogger
							.error("NO CONCEPT ID: Invalid or no concept_id for answer. Value="
									+ conceptQuestionId
									+ ";Coded Name: "
									+ conceptName + ";MRN:" + pIdentifierString);
				}
				return answer;
			} catch (RuntimeException e)
			{
				logger.error("createObs() failed. MRN: " + pIdentifierString
						+ ";Invalid OBX value: " + stConceptId
						+ ";concept question id: " + conceptQuestionId
						+ "; concept name: " + conceptName + ";long name = "
						+ conceptLongName);
			}

		}
		return null;
	}

	protected Concept processCEType(Varies value, Logger logger,
			String pIdentifierString, String conceptQuestionId,
			Logger conceptNotFoundLogger)
	{
		String conceptName = ((CE) value.getData()).getText().toString();
		String stConceptId = ((CE) value.getData()).getIdentifier().toString();
		Integer intObxValueID = null;

		try
		{
			intObxValueID = Integer.parseInt(stConceptId);
		} catch (NumberFormatException ne)
		{
			logger
					.warn("PARSE WARNING: Unable to parse integer from CE observation value id string - Invalid value in OBX  = "
							+ stConceptId
							+ " for patient MRN "
							+ pIdentifierString
							+ " \n concept question id : "
							+ conceptQuestionId
							+ " concept name: "
							+ conceptName + " Check coded value.");
		}

		// Success conversion to int
		if (intObxValueID != null)
		{
			try
			{
				Concept answer = Util.lookupConcept(intObxValueID, conceptName);
				if (answer == null)
				{
					conceptNotFoundLogger
							.error("NO CONCEPT ID: Invalid or no concept_id for answer. Value="
									+ conceptQuestionId
									+ ";Coded Name: "
									+ conceptName + ";MRN:" + pIdentifierString);
				}
				return answer;
			} catch (RuntimeException e)
			{
				logger.error("createObs() failed. MRN: " + pIdentifierString
						+ ";Invalid OBX value: " + stConceptId
						+ ";concept question id: " + conceptQuestionId
						+ "; concept name: " + conceptName);
			}

		}
		return null;
	}

	public Concept getCodedResult(Message message, int orderRep, int obxRep,
			Logger logger, String pIdentifierString, String obsvID,
			String obsValueType, Logger conceptNotFoundLogger)
	{
		Varies[] values = getOBX(message, orderRep, obxRep)
				.getObservationValue();
		Varies value = null;
		Concept conceptResult = null;

		if (values.length > 0)
		{
			value = values[0];

			if (obsValueType.equals("CWE"))
			{
				conceptResult = processCWEType(value, logger,
						pIdentifierString, obsvID, conceptNotFoundLogger);
			}
			if (obsValueType.equals("CE"))
			{
				conceptResult = processCEType(value, logger, pIdentifierString,
						obsvID, conceptNotFoundLogger);
			}
		}

		return conceptResult;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openmrs.module.sockethl7listener.ObsHandler#getObs(ca.uhn.hl7v2.model.Message)
	 */
	public ArrayList<Obs> getObs(Message message, Patient patient) throws HL7Exception
	{
		// TODO Auto-generated method stub
		return null;
	}

	public int getReps(Message message)
	{
		int reps = 0;
		if (message instanceof ORU_R01)
		{
			reps = ((ORU_R01) message).getPATIENT_RESULT()
					.getORDER_OBSERVATION().getOBSERVATIONReps();
		} else if ((message instanceof ADT_A01))
		{
			reps = ((ADT_A01) message).getOBXReps();
		}
		return reps;
	}
	
	/**
	 * @see org.openmrs.module.sockethl7listener.HL7ObsHandler#getUnits(Message, int, int)
	 * DWE CHICA-635
	 */
	public String getUnits(Message message, int orderRep, int obxRep)
	{
		OBX obx = getOBX(message, orderRep, obxRep);
    	if(obx != null){
    		CE units = obx.getUnits();
    		return units.getText().toString();
    	}
    				
		return "";
	}
}
