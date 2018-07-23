package org.openmrs.module.sockethl7listener;

import java.util.Date;

import ca.uhn.hl7v2.model.Message;

/**
 * @author Dave Ely
 */
public interface HL7EventTypeHandler 
{
	/**
	 * Get event type code from EVN-1
	 * @param message
	 * @return String value of event type code
	 */
	public String getEventTypeCode(Message message);
	
	/**
	 * Get recorded date/time from EVN-2
	 * @param message
	 * @return Date representation of TS data type for recorded date/time 
	 */
	public Date getRecordedDateTime(Message message);
	
	/**
	 * Get date/time planned event from EVN-3
	 * @param message
	 * @return Date representation of TS data type for date/time planned event
	 */
	public Date getDateTimePlannedEvent(Message message);
	
	/**
	 * Get event reason code from EVN-4
	 * @param message
	 * @return String value of event reason code
	 */
	public String getEventReasonCode(Message message);
	
	/**
	 * Get operator ID from EVN-5.1
	 * @param message
	 * @return String value of operator ID
	 */
	public String getOperatorId(Message message);
	
	/**
	 * Get event occurred from EVN-6
	 * @param message
	 * @return Date representation of TS data type for event occurred
	 */
	public Date getEventOccurred(Message message);
	
	/**
	 * Get event facility from EVN-7
	 * @param message
	 * @return String value of event facility
	 */
	public String getEventFacility(Message message);
}
