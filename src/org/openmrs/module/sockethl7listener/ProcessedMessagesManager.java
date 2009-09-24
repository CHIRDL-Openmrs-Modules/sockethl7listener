/**
 * 
 */
package org.openmrs.module.sockethl7listener;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.api.context.Context;
import org.openmrs.module.sockethl7listener.service.SocketHL7ListenerService;

/**
 * @author Tammy Dugan
 * 
 */

public class ProcessedMessagesManager
{
	private static Log log = LogFactory.getLog(ProcessedMessagesManager.class);

	private final static Lock lock = new ReentrantLock();
	private final static Condition goProcessEncounter = lock.newCondition();
	private static Boolean messageBeingProcessedByAOP = false;

	public static void messageProcessed(Encounter encounter)
	{
		lock.lock();
		try
		{
			while (messageBeingProcessedByAOP)
			{
				log
						.error("Waiting for encounter process in AOP to finish.....");

				try
				{
					goProcessEncounter.await();
				} catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			messageBeingProcessedByAOP = true;
			SocketHL7ListenerService socketHL7ListenerService = Context
					.getService(SocketHL7ListenerService.class);
			socketHL7ListenerService.messageProcessed(encounter);
		} finally
		{
			lock.unlock();
		}
	}

	public static void encountersProcessed()
	{
		messageBeingProcessedByAOP = false;
		goProcessEncounter.signalAll();
	}

}
