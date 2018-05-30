package org.openmrs.module.sockethl7listener;

import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.chirdlutil.util.ChirdlUtilConstants;


/**
 * Encounter provider class specific to information from PV1 segment of hl7 messages.
 * @author msheley
 *
 */
public class Provider {

	private String firstName;
	private String lastName;
	private String ehrProviderId; // NOTE: This is the id from the HL7 message
	private Integer providerId; // NOTE: This is the value from the provider_id column in the provider table
	private String poc;
	private String pocFacility;
	private String pocRoom;
	private String pocBed;
	private String admitSource;
	private static final Log LOG =  LogFactory.getLog(Provider.class);
	private static final String VOIDED_REASON_PERSON_NAME_CHANGE = "voided due to name update in HL7 message";
	
	
	public Provider (){
		firstName = "";
		lastName = "";
		ehrProviderId = "";
		
	}
	
	public Provider (String observationName){
		firstName = "";
		lastName = "";
		ehrProviderId = "";
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = org.openmrs.module.chirdlutil.util.Util.toProperCase(firstName);
		
	}
	
	/**
	 * // NOTE: This is the id from the HL7 message
	 * @return ehrProviderId
	 */
	public String getEhrProviderId() {
		return ehrProviderId;
	}
	
	/**
	 * // NOTE: This is the id from the HL7 message
	 * @param ehrProviderId to set
	 */
	public void setEhrProviderId(String ehrProviderId) {
		this.ehrProviderId = ehrProviderId;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = org.openmrs.module.chirdlutil.util.Util.toProperCase(lastName);
	}
	
	/**
	 * // NOTE: This is the value from the provider_id column in the provider table
	 * @return providerId
	 */
	public Integer getProviderId(){
		return this.providerId;
	}
	
	/**
	 * // NOTE: This is the value from the provider_id column in the provider table
	 * @param providerId the providerId to set
	 */
	public void setProviderId(Integer providerId){
		this.providerId = providerId;
	}
	
	/** Parses Provider information from PROVIDER_NAME observation in past
	 * encounters that happened on the same date.
	 * Split using " ";
	 * @param all
	 */
	public void parseProviderFromObs(String prov){
		String firstname = "", lastname = "";
		int index1 =prov.indexOf(".");
		if (index1 != -1){
			firstname = prov.substring(0,index1);
			lastname = prov.substring(index1 + 1);
		}else {
			firstname = prov;
		}
		
		this.setFirstName(firstname);
		this.setLastName(lastname);
		
	}
	
   public PersonName parseProviderNameFromAttribute(PersonAttribute provNameAttr) {
		

		String provNameValue = provNameAttr.getValue();
		String firstname = "";
		String lastname = "";
		
		int index1 =provNameValue.indexOf(".");
		if (index1 != -1){
			firstname = provNameValue.substring(0,index1);
			lastname = provNameValue.substring(index1 + 1);
		}else {
			firstname = provNameValue;
		}
		PersonName provName = new PersonName();
		provName.setFamilyName(lastname);
		provName.setGivenName(firstname);
		
		return provName;
   }
	
	public boolean equals(Provider p){
		boolean ret = false;
        if (p!= null){
			if (firstName.equalsIgnoreCase(p.getFirstName().trim())
					&& lastName.equalsIgnoreCase(p.getLastName().trim())){
				ret = true;
			}
        }
	
		return ret;
	}
	
	
	/**
	 * CHICA-221 Updated method to use the ProviderService
	 * @param provider
	 * @return
	 */
	public org.openmrs.Provider createProvider(Provider provider)  {

		org.openmrs.Provider openmrsProvider = null;
		ProviderService providerService = Context.getProviderService();
		
		try {
			String firstname = provider.getFirstName();
			String lastname = provider.getLastName();
			String providerId = provider.getEhrProviderId();
			
			if(firstname == null){
				firstname = "";
			}
			if(lastname == null){
				lastname = "";
			}
			if(providerId == null){
				providerId = "";
			}
				
			// Determine if the provider already exists
			// Look the provider up using the provider id found in the HL7 message
			if(!providerId.isEmpty()){
				// Identifier is now stored in the provider table. This was migrated as part of the openmrs upgrade
				openmrsProvider = providerService.getProviderByIdentifier(providerId); 
				if(openmrsProvider == null){
					// Provider does not exist, create a new one
					openmrsProvider = new org.openmrs.Provider();
					Person newPerson = new Person();
					newPerson.setGender(ChirdlUtilConstants.GENDER_UNKNOWN);
					PersonName newName = new PersonName(firstname, "", lastname);
					newPerson.addName(newName);
					openmrsProvider.setPerson(newPerson);
					openmrsProvider.setIdentifier(providerId);
					openmrsProvider = providerService.saveProvider(openmrsProvider);	
				}
				else{
					Person person = openmrsProvider.getPerson();
					PersonName personName = person.getPersonName();
					
					// Make sure the name matches. If it does not, void the current one and create a new one
					if(!personName.getFamilyName().equalsIgnoreCase(lastname) || !personName.getGivenName().equalsIgnoreCase(firstname)){
						// Name has changed, retire the current name and create a new one
						personName.setVoided(true);
						personName.setPreferred(false);
						personName.setVoidedBy(Context.getAuthenticatedUser());
						personName.setDateVoided(new Date());
						personName.setVoidReason(VOIDED_REASON_PERSON_NAME_CHANGE);
							
						PersonName newName = new PersonName(firstname, "", lastname);
						newName.setPreferred(true);
						newName.setCreator(Context.getAuthenticatedUser());
						
						person.addName(newName);
						
						openmrsProvider = providerService.saveProvider(openmrsProvider);
					}
				}
				
				if(openmrsProvider != null && openmrsProvider.getId() != null){
					provider.setProviderId(openmrsProvider.getId());
				}
			}
			else{
				// Use the global property to determine which provider to use when the attending provider field is empty in the HL7 message
				AdministrationService adminService = Context.getAdministrationService();
				String unknownProviderIdString = adminService.getGlobalProperty(ChirdlUtilConstants.GLOBAL_PROP_UNKNOWN_PROVIDER_ID);
				if(unknownProviderIdString == null || unknownProviderIdString.trim().length() == 0){
					LOG.error("No value set for global property: " + ChirdlUtilConstants.GLOBAL_PROP_UNKNOWN_PROVIDER_ID + ". This will prevent the encounter from being created.");
					return null;
				}
				
				try{
					Integer unknownProviderId = Integer.parseInt(unknownProviderIdString);
					openmrsProvider = providerService.getProvider(unknownProviderId);
				}
				catch(NumberFormatException nfe){
					LOG.error("Invalid number format for global property " + ChirdlUtilConstants.GLOBAL_PROP_UNKNOWN_PROVIDER_ID + ". This will prevent the encounter from being created.");
					return null;
				}	
			}
		} catch (Exception e){
			LOG.error("Error while creating or updating a provider.", e);
		}
		
		return openmrsProvider;
	}

	/**
	 * CHICA-221 Updated method to use the ProviderService and return org.openmrs.Provider
	 * @param provider
	 * @return
	 * @throws APIException
	 */
	public org.openmrs.Provider getProvider(Provider provider) throws APIException {
	   
		ProviderService providerService = Context.getProviderService();
        Integer providerId = provider.getProviderId();
        org.openmrs.Provider openmrsProvider = null;
		if (providerId != null) {
			openmrsProvider = providerService.getProvider(providerId);
		}
		if (providerId == null || openmrsProvider == null){
			openmrsProvider = createProvider(provider);
        }

		return openmrsProvider;
	}
	
	

	/**
	 * CHICA-221 Updated method to use ProviderService and org.openmrs.Provider
	 * @param openmrsProvider
	 */
	public void setProvider(org.openmrs.Provider openmrsProvider)  {
	
		if (openmrsProvider == null){
			openmrsProvider = createProvider(this);
		}	
		
		Person person = openmrsProvider.getPerson();
	    setFirstName(person.getGivenName());
	    setLastName(person.getFamilyName());
	    String identifier = openmrsProvider.getIdentifier();
	    if (identifier == null) {
	    	setEhrProviderId("");
	    }else{
	    	setEhrProviderId(identifier);
	    }
	
	}
	
	/**
	 * @return the poc
	 */
	public String getPoc() {
		return poc;
	}

	/**
	 * @param poc the poc to set
	 */
	public void setPoc(String poc) {
		this.poc = poc;
	}

	/**
	 * @return the pocFacility
	 */
	public String getPocFacility() {
		return pocFacility;
	}

	/**
	 * @param pocFacility the pocFacility to set
	 */
	public void setPocFacility(String pocFacility) {
		this.pocFacility = pocFacility;
		
	}

	/**
	 * @return the pocRoom
	 */
	public String getPocRoom() {
		return pocRoom;
	}

	/**
	 * @param pocRoom the pocRoom to set
	 */
	public void setPocRoom(String pocRoom) {
		this.pocRoom = pocRoom;
	}

	/**
	 * @return the pocBed
	 */
	public String getPocBed() {
		return pocBed;
	}

	/**
	 * @param pocBed the pocBed to set
	 */
	public void setPocBed(String pocBed) {
		this.pocBed = pocBed;
		
	}
	
	/**
	 * @return the admitSource
	 */
	public String getAdmitSource() {
		return admitSource;
	}

	/**
	 * @param admitSource the admitSource to set
	 */
	public void setAdmitSource(String admitSource) {
		this.admitSource = admitSource;
	}
}
