package org.openmrs.module.sockethl7listener;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.hl7.HL7InQueue;
import org.openmrs.hl7.HL7Service;
import org.openmrs.hl7.HL7Source;
import org.openmrs.module.chirdlutil.threadmgmt.RunnableResult;
import org.openmrs.module.sockethl7listener.util.Util;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.app.ApplicationException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.parser.Parser;

/**
 * Runnable to process vitals HL7 message.
 * 
 * @author Steve McKee
 */
public class ProcessMessageRunnable implements RunnableResult<Message> {
	
	private static final Logger log =  LoggerFactory.getLogger("SocketHandlerLogger");
	private Message message;
	private Message response;
	private HL7SocketHandler socketHandler;
	private HL7EncounterHandler hl7EncounterHandler;
	private Parser parser;
	private List<HL7Filter> filters;
	private HashMap<String, Object> parameters;
	private Exception exception;
	//MessageState 0=pending, 1=processing, 2=processed, 3=error
	private static final Integer PROCESSING = Integer.valueOf(1);
	private static final Integer PROCESSED = Integer.valueOf(2);
	
	/**
	 * Constructor method
	 * 
	 * @param message The HL7 message to process
	 * @param socketHandler The socket handler used to process the message
	 * @param hl7EncounterHandler The encounter handler used to process the message
	 * @param parser The parser used to parse the message
	 * @param filters Any filters that need to be applied
	 * @param parameters Map of parameters
	 */
	public ProcessMessageRunnable(Message message, HL7SocketHandler socketHandler, HL7EncounterHandler hl7EncounterHandler,
	    Parser parser, List<HL7Filter> filters, HashMap<String, Object> parameters) {
		this.message = message;
		this.socketHandler = socketHandler;
		this.hl7EncounterHandler = hl7EncounterHandler;
		this.parser = parser;
		this.filters = filters;
		this.parameters = parameters;
	}
	
	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		boolean error = false;
		
			HL7Service hl7Service = Context.getHL7Service();
			String incomingMessageString = "";
			try {
				incomingMessageString = this.parser.encode(this.message);
			}
			catch (HL7Exception e) {
				log.error("Exception encoding hl7 message.", e);
				this.exception = e;
			}
			
			if (!(this.message instanceof ORU_R01) && !(this.message instanceof ADT_A01)) {
				String messageType = "";
				
				if (this.message.getParent() != null) {
					messageType = this.message.getParent().getName();
				}
				
				this.exception = new ApplicationException("Invalid message type (" + messageType
				        + ") sent to HL7 Socket handler. Only ORU_R01 and ADT_A01 valid currently. ");
				return;
			}
			
			try {
				HL7Source hl7Source = new HL7Source();
				
				if (hl7Service.getHL7SourceByName(this.socketHandler.getPort().toString()) == null) {
					hl7Source.setName(String.valueOf(this.socketHandler.getPort()));
					hl7Source.setDescription("Port for hl7 message.");
					hl7Service.saveHL7Source(hl7Source);
				} else {
					hl7Source = hl7Service.getHL7SourceByName(this.socketHandler.getPort().toString());
				}
				
				HL7InQueue hl7inQ = new HL7InQueue();
				hl7inQ.setHL7Source(hl7Source);
				hl7inQ.setHL7Data(incomingMessageString);
				//MessageState 0=pending, 1=processing, 2=processed, 3=error
				hl7inQ.setMessageState(PROCESSING);
				HL7InQueue savedHl7 = hl7Service.saveHL7InQueue(hl7inQ);
				
				this.socketHandler.archiveHL7Message(incomingMessageString);
				
				boolean ignoreMessage = false;
				
				if (this.filters != null) {
					for (HL7Filter filter : this.filters) {
						if (filter.ignoreMessage(this.hl7EncounterHandler, this.message, incomingMessageString)) {
							ignoreMessage = true;
							break;
						}
					}
				}
				
				if (!ignoreMessage) {
					error = this.socketHandler.processMessageSegments(this.message, incomingMessageString, this.parameters);
				}
				try {
					MSH msh = HL7ObsHandler25.getMSH(this.message);
					this.response = Util.makeACK(msh, error, null, null);
				}
				catch (Exception e) {
					log.error("Exception sending ACK message.", e);
					this.exception = e;
				}
				
				Context.clearSession();
				
				savedHl7.setMessageState(PROCESSED);
				Context.getHL7Service().saveHL7InQueue(savedHl7);
				
			}
			catch (ContextAuthenticationException e) {
				log.error("Context Authentication exception: ", e);
				this.exception = e;
			}
			catch (ClassCastException e) {
				log.error("Error casting to {} ", this.message.getClass().getName(), e);
				this.exception = new ApplicationException("Invalid message type for handler");
			}
			catch (Exception e) {
				log.error("Exception processing hl7 message.", e);
				this.exception = e;
			}
			finally {
				if (this.response == null) {
					try {
						error = true;
						MSH msh = HL7ObsHandler25.getMSH(this.message);
						this.response = Util.makeACK(msh, error, null, null);
					}
					catch (Exception e) {
					    log.error("Second attempt to send ACK message failed.", e);
						this.exception = e;
					}
				}
			}
	}
	
	/**
	 * @see org.openmrs.module.chirdlutil.threadmgmt.RunnableResult#getResult()
	 */
	@Override
	public Message getResult() {
		return this.response;
	}
	
	/**
	 * @see org.openmrs.module.chirdlutil.threadmgmt.RunnableResult#getException()
	 */
	@Override
	public Exception getException() {
		return this.exception;
	}
	
}
