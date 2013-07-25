/**
 * 
 */
package org.openmrs.module.sockethl7listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.scheduler.TaskDefinition;
import org.openmrs.scheduler.tasks.AbstractTask;

import ca.uhn.hl7v2.llp.LowerLayerProtocol;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.validation.impl.NoValidation;

/**
 * @author Tammy Dugan
 * 
 */
public class TestSimpleServer extends AbstractTask
{
	private Log log = LogFactory.getLog(this.getClass());
	private SimpleServer server = null;
	
	@Override
	public void initialize(TaskDefinition config)
	{
		super.initialize(config);
		AdministrationService adminService = Context.getAdministrationService();

		String portString = this.taskDefinition.getProperty("port");

		if (portString == null)
		{
			portString = adminService
					.getGlobalProperty("sockethl7listener.port");
		}

		try
		{
			Integer port = null;
			
			try
			{
				port = Integer.parseInt(portString);
			} catch (Exception e)
			{
				this.log.error("Error parsing port: "+port);
			}
			PatientHandler patientHandler = new PatientHandler();
			PipeParser parser = new PipeParser();
			parser.setValidationContext(new NoValidation());
			HL7SocketHandler socketHandler = new HL7SocketHandler(parser,
					patientHandler,new HL7ObsHandler25(),
					new HL7EncounterHandler25(), new HL7PatientHandler25(),null);
			
			this.server = new SimpleServer(port, LowerLayerProtocol
					.makeLLP(), parser,patientHandler,socketHandler);
			System.out.println("Starting SimpleServer...");
		} catch (Exception e)
		{
			System.out.println("Error starting SimpleServer...");
			e.printStackTrace();
		}
	}

	@Override
	public void execute()
	{
		Context.openSession();
		try
		{
			this.server.start();
		} catch (Exception e)
		{
			e.printStackTrace();
		} finally
		{
			Context.closeSession();
		}

	}
	
	@Override
	public void shutdown()
	{
		super.shutdown();
		try
		{
			this.server.stop();
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
