package org.openmrs.module.sockethl7listener;

import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.BaseModuleActivator;
import org.openmrs.module.chirdlutil.util.ChirdlUtilConstants;

/**
 * Purpose: Checks that module specific global properties have been set 
 *
 * @author Tammy Dugan
 *
 */
public class SocketHL7ListenerActivator extends BaseModuleActivator {

	private Log log = LogFactory.getLog(this.getClass());

	/**
	 * @see org.openmrs.module.BaseModuleActivator#started()
	 */
	public void started() {
		this.log.info("Starting HL7 Listener Module");
		
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
						this.log.error("You must set a value for global property: "
								+ currName);
					}
				}
			}
		} catch (Exception e)
		{
			this.log.error("Error checking global properties for hl7 listener module");

		}
	}
	
	/**
	 * Sets up configuration for Hapi
	 */
	private void configureHapi() {
		String charEncoding = Context.getAdministrationService().getGlobalProperty(ChirdlUtilConstants.GLOBAL_PROP_HAPI_CHARACTER_ENCODING);
		if (StringUtils.isEmpty(charEncoding) || StringUtils.isWhitespace(charEncoding)) {
			log.warn("Global property " + ChirdlUtilConstants.GLOBAL_PROP_HAPI_CHARACTER_ENCODING + " is not set.  Hapi's default " +
				"character encoding will be used.");
		} else {
			System.setProperty(ChirdlUtilConstants.HAPI_CHARSET_PROPERTY_KEY, charEncoding);
		}
	}
	
	/**
	 * @see org.openmrs.module.BaseModuleActivator#stopped()
	 */
	public void stopped() {
		this.log.info("Shutting down HL7 Listener Module");
	}

}
