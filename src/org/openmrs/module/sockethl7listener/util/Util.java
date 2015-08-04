/**
 * 
 */
package org.openmrs.module.sockethl7listener.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import org.openmrs.Concept;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.chirdlutil.util.ChirdlUtilConstants;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.primitive.CommonTS;
import ca.uhn.hl7v2.sourcegen.SourceGenerator;
import ca.uhn.hl7v2.util.MessageIDGenerator;
import ca.uhn.hl7v2.util.Terser;

/**
 * @author tmdugan
 * 
 */
public class Util
{	
	public static Concept lookupConcept(Integer conceptId,String conceptName)
	{
		ConceptService cs = Context.getConceptService();
		
		//lookup by concept id
		Concept concept = cs.getConcept(conceptId);
		
		if(concept == null)
		{
			concept = cs.getConceptByName(conceptName);
		}
		
		return concept;
	}
	
	public static String convertDateToString(Date date){
		String dateStr = "";
		
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");
		if (date != null) { 
			dateStr = df.format(date);
		}
		return dateStr;
	}
	
	
	/*
	 *Creates an ACK message with the minimum required information from an inbound message header
	 * @param inboundHeader - MSH segment
	 * @param error - boolean indicating error when processing
	 * @param errorText - Error text for the MSA-3 field
	 * @param acceptText -Accept text for the MSA-3 field
	 * @return
	 * @throws HL7Exception
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static Message makeACK(Segment inboundHeader, boolean error, String errorText, String acceptText) throws HL7Exception,
	IOException {
		if (!inboundHeader.getName().equals(ChirdlUtilConstants.HL7_SEGMENT_MESSAGE_HEADER_MSH))
			throw new HL7Exception(
					"Need an MSH segment to create a response ACK (got "
					+ inboundHeader.getName() + ")");

		// make ACK of correct version
		String version = null;
		try {
			version = Terser.get(inboundHeader, 12, 0, 1, 1);
		} catch (HL7Exception e) { /* proceed with null */
		}
		if (version == null)
			version = "2.5";

		String ackClassName = SourceGenerator.getVersionPackageName(version)
		+ "message.ACK";

		Message ackMessage = null;
		try {
			Class ackClass = Class.forName(ackClassName);
			ackMessage = (Message) ackClass.newInstance();
		} catch (Exception e) {
			throw new HL7Exception("Can't instantiate ACK of class "
					+ ackClassName + ": " + e.getClass().getName());
		}
		String test = ChirdlUtilConstants.HL7_SEGMENT_MESSAGE_HEADER_MSH;
		// populate outbound MSH using data from inbound message ...
		Segment outboundHeader = (Segment) ackMessage.get(ChirdlUtilConstants.HL7_SEGMENT_MESSAGE_HEADER_MSH);
		fillResponseHeader(inboundHeader, outboundHeader, version);
		
		// Populate MSA segment for ACK message
		fillDetails(ackMessage, inboundHeader, error, errorText, acceptText);
	
		return ackMessage;
	}

	/**
	 * Populates certain required fields in a response message header, using
	 * information from the corresponding inbound message. The current time is
	 * used for the message time field, and <code>MessageIDGenerator</code> is
	 * used to create a unique message ID. Version and message type fields are
	 * not populated.
	 */
	public static void fillResponseHeader(Segment inboundHeader, Segment outboundHeader, String version)
	throws HL7Exception, IOException {
		
		
		
		// get MSH data from incoming message ...
		String encChars = Terser.get(inboundHeader, 2, 0, 1, 1);
		String fieldSep = Terser.get(inboundHeader, 1, 0, 1, 1);
		String procID = Terser.get(inboundHeader, 11, 0, 1, 1);
		String sendingApp = Terser.get(inboundHeader, 3, 0, 1, 1);

		// populate outbound MSH using data from inbound message ...
		Terser.set(outboundHeader, 2, 0, 1, 1, encChars);
		Terser.set(outboundHeader, 1, 0, 1, 1, fieldSep);
		GregorianCalendar now = new GregorianCalendar();
		now.setTime(new Date());
		Terser.set(outboundHeader, 7, 0, 1, 1, CommonTS.toHl7TSFormat(now));
		Terser.set(outboundHeader, 10, 0, 1, 1, MessageIDGenerator.getInstance()
				.getNewID());
		Terser.set(outboundHeader, 11, 0, 1, 1, procID);
		Terser.set(outboundHeader, 3, 0, 1, 1, sendingApp);
		Terser.set(outboundHeader, 9, 0, 1, 1, "ACK");
		Terser.set(outboundHeader, 12, 0, 1, 1, version);
		
	}

	/**
	 * Fills in the details of an Application Reject message, including response
	 * and error codes, and a text error message. This is the method to override
	 * if you want to respond differently.
	 */
	public static void fillDetails(Message ack, Segment inboundHeader, boolean error, String errorText, String acceptText) throws HL7Exception
	{
		Segment msa = (Segment) ack.get(ChirdlUtilConstants.HL7_SEGMENT_MESSAGE_ACKNOWLEDGMENT_MSA);
		// populate MSA and ERR with generic error ...
		if (error) {
			Terser.set(msa, 1, 0, 1, 1, ChirdlUtilConstants.HL7_ACK_CODE_APPLICATION_ERROR);
			if (errorText == null || errorText.equals("")){
				errorText = "Application Error";
			}
			Terser.set(msa, 3, 0, 1, 1, errorText);
			
		} else {
			Terser.set(msa, 1, 0, 1, 1, ChirdlUtilConstants.HL7_ACK_CODE_APPLICATION_ACCEPT);
			if (acceptText == null || acceptText.equals("")){
				acceptText = "Message received successfully";
			}
			Terser.set(msa, 3, 0, 1, 1, acceptText);			
		}
	}

}
