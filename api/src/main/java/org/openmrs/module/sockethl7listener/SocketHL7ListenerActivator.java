package org.openmrs.module.sockethl7listener;

import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.DaemonToken;
import org.openmrs.module.DaemonTokenAware;
import org.openmrs.module.chirdlutil.util.ChirdlUtilConstants;
import org.openmrs.module.sockethl7listener.util.Util;

/**
 * Purpose: Checks that module specific global properties have been set 
 *
 * @author Tammy Dugan
 *
 */
public class SocketHL7ListenerActivator extends BaseModuleActivator implements DaemonTokenAware {

	private static final Logger log =  LoggerFactory.getLogger("SocketHandlerLogger");

	/**
	 * @see org.openmrs.module.BaseModuleActivator#started()
	 */
	@Override
	public void started() {
		log.info("Starting HL7 Listener Module");
		
		//check that all the required global properties are set
		checkGlobalProperties();
		
		// configure Hapi
		configureHapi();
	}

	private void checkGlobalProperties()
	{
		try
		{
			AdministrationService adminService = Context.getAdministrationService();
			 
			Iterator<GlobalProperty> properties = adminService
					.getAllGlobalProperties().iterator();
			GlobalProperty currProperty = null;
			String currValue = null;
			String currName = null;

			while (properties.hasNext())
			{
				currProperty = properties.next();
				currName = currProperty.getProperty();
				if (currName.startsWith("sockethl7listener"))
				{
					currValue = currProperty.getPropertyValue();
					if (currValue == null || currValue.length() == 0)
					{
						log.error("Global property {} has no value", currName);
					}
				}
			}
		} catch (Exception e)
		{
			log.error("Exception checking global properties for hl7 listener module", e);
		}
	}
	
	/**
	 * Sets up configuration for Hapi
	 */
	private void configureHapi() {
		String charEncoding = Context.getAdministrationService().getGlobalProperty(ChirdlUtilConstants.GLOBAL_PROP_HAPI_CHARACTER_ENCODING);
		if (StringUtils.isEmpty(charEncoding) || StringUtils.isWhitespace(charEncoding)) {
			log.error("Global property {} is not set. Hapi default character encoding will be used.", ChirdlUtilConstants.GLOBAL_PROP_HAPI_CHARACTER_ENCODING);
		} else {
			System.setProperty(ChirdlUtilConstants.HAPI_CHARSET_PROPERTY_KEY, charEncoding);
		}
	}
	
	/**
	 * @see org.openmrs.module.BaseModuleActivator#stopped()
	 */
	@Override
	public void stopped() {
		log.info("Shutting down HL7 Listener Module");
	}

	/**
	 * 
	 * @see org.openmrs.module.DaemonTokenAware#setDaemonToken(org.openmrs.module.DaemonToken)
	 */
	@Override
	public void setDaemonToken(DaemonToken token) {
		Util.setDaemonToken(token);
	}

}
