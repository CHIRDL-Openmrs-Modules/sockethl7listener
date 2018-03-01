package org.openmrs.module.sockethl7listener;

import java.util.Date;

import org.openmrs.module.chirdlutil.util.ChirdlUtilConstants;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.datatype.TS;
import ca.uhn.hl7v2.model.v25.datatype.XCN;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.segment.EVN;

/**
 * @author Dave Ely
 *
 */
public class HL7EventTypeHandler25 implements HL7EventTypeHandler
{

	/**
	 * @see org.openmrs.module.sockethl7listener.HL7EventTypeHandler#getEventTypeCode(ca.uhn.hl7v2.model.Message)
	 */
	@Override
	public String getEventTypeCode(Message message) {
		String eventTypeCode = ChirdlUtilConstants.GENERAL_INFO_EMPTY_STRING;
		EVN evn = getEVN(message);
		if(evn != null){
			eventTypeCode = evn.getEventTypeCode().getValue();
		}
		
		return eventTypeCode;
	}

	/**
	 * @see org.openmrs.module.sockethl7listener.HL7EventTypeHandler#getRecordedDateTime(ca.uhn.hl7v2.model.Message)
	 */
	@Override
	public Date getRecordedDateTime(Message message) {
		
		Date dateTime = null;
		EVN evn = getEVN(message);
		if(evn != null){
			TS timeStamp = null;
			timeStamp = evn.getRecordedDateTime();
			dateTime = HL7ObsHandler25.TranslateDate(timeStamp);
		}
		
		return dateTime;
	}

	/**
	 * @see org.openmrs.module.sockethl7listener.HL7EventTypeHandler#getDateTimePlannedEvent(ca.uhn.hl7v2.model.Message)
	 */
	@Override
	public Date getDateTimePlannedEvent(Message message) {
		Date dateTime = null;
		EVN evn = getEVN(message);
		if(evn != null){
			TS timeStamp = null;
			timeStamp = evn.getDateTimePlannedEvent();
			dateTime = HL7ObsHandler25.TranslateDate(timeStamp);
		}
		
		return dateTime;
	}

	/**
	 * @see org.openmrs.module.sockethl7listener.HL7EventTypeHandler#getEventReasonCode(ca.uhn.hl7v2.model.Message)
	 */
	@Override
	public String getEventReasonCode(Message message) {
		String eventReasonCode = ChirdlUtilConstants.GENERAL_INFO_EMPTY_STRING;
		EVN evn = getEVN(message);
		if(evn != null){
			eventReasonCode = evn.getEventReasonCode().getValue();
		}
		
		return eventReasonCode;
	}

	/**
	 * @see org.openmrs.module.sockethl7listener.HL7EventTypeHandler#getOperatorId(ca.uhn.hl7v2.model.Message)
	 */
	@Override
	public String getOperatorId(Message message) {
		String operatorIdString = ChirdlUtilConstants.GENERAL_INFO_EMPTY_STRING;
		EVN evn = getEVN(message);
		if(evn != null){
			XCN[] xcnArray = evn.getOperatorID();
			if(xcnArray != null && xcnArray.length > 0)
			{
				XCN xcn = xcnArray[0];
				operatorIdString = xcn.getIDNumber().getValue();
			}
		}
		
		return operatorIdString;
	}

	/**
	 * @see org.openmrs.module.sockethl7listener.HL7EventTypeHandler#getEventOccurred(ca.uhn.hl7v2.model.Message)
	 */
	@Override
	public Date getEventOccurred(Message message) {
		Date dateTime = null;
		EVN evn = getEVN(message);
		if(evn != null){
			TS timeStamp = null;
			timeStamp = evn.getEventOccurred();
			dateTime = HL7ObsHandler25.TranslateDate(timeStamp);
		}
		
		return dateTime;
	}

	/**
	 * @see org.openmrs.module.sockethl7listener.HL7EventTypeHandler#getEventFacility(ca.uhn.hl7v2.model.Message)
	 */
	@Override
	public String getEventFacility(Message message) {
		String eventFacility = ChirdlUtilConstants.GENERAL_INFO_EMPTY_STRING;
		EVN evn = getEVN(message);
		if(evn != null){
			eventFacility = evn.getEventFacility().getUniversalID().getValue();
		}
		
		return eventFacility;
	}

	/**
	 * Get EVN segment - currently only supported for ADT
	 * @param message
	 * @return
	 */
	private EVN getEVN(Message message)
	{
		if (message instanceof ADT_A01)
		{
			return getEVN((ADT_A01) message);
		}
		return null;
	}
		
	/**
	 * Get EVN segment from ADT message
	 * @param adt
	 * @return
	 */
	private EVN getEVN(ADT_A01 adt)
	{ 
		return adt.getEVN();
	}
}
