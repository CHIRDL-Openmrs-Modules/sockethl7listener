package org.openmrs.module.sockethl7listener;

import java.util.Date;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;


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
	public AdministrationService as;
	private static final String PROVIDER_ID = "Provider ID";
	private Log log =  LogFactory.getLog(this.getClass());
	
	
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
		

		String ProvNameValue = provNameAttr.getValue();
		String firstname = "";
		String lastname = "";
		
		int index1 =ProvNameValue.indexOf(".");
		if (index1 != -1){
			firstname = ProvNameValue.substring(0,index1);
			lastname = ProvNameValue.substring(index1 + 1);
		}else {
			firstname = ProvNameValue;
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

		org.openmrs.Provider savedProvider = null;
		boolean changed = false;
		PersonService ps = Context.getPersonService();
		
		try {
			String firstname = provider.getFirstName();
			String lastname = provider.getLastName();
			String providerId = provider.getEhrProviderId();
			String fn = "";
			String ln = "";
			
			if(firstname == null){
				firstname = "";
			}
			if(lastname == null){
				lastname = "";
			}
			if(providerId == null){
				providerId = "";
			}
			if(firstname.contains(" ")){
				fn = firstname.replace(" ", "_");
			}
			else {
				fn = firstname;
			}
			if(lastname.contains(" ")){
				ln = lastname.replace(" ", "_");
			}
			else{
				ln = lastname;
			}
				
			
			// Determine if the provider already exists
			ProviderService providerService = Context.getProviderService();
			List<org.openmrs.Provider> providers = providerService.getAllProviders();
			org.openmrs.Provider openmrsProvider = null;

			// Match provider on first and last name
			// TODO CHICA-221 What about cases where the name may or may not contain spaces
			// Some users in the EHR has multiple user names because of names that contain spaces
			// TODO CHICA-221 Fix inconsistency between what is stored in the DB and what we are checking here using fn and ln variable. 
			// Below stores the name using firstname and lastname variables, which would contain spaces instead of "_" as shown above
			for(org.openmrs.Provider currProvider : providers){
				Person person = currProvider.getPerson();
				if(fn.toLowerCase().equals(person.getGivenName().toLowerCase()) && ln.toLowerCase().equals(person.getFamilyName().toLowerCase())){
					openmrsProvider = currProvider;
					break;
				}
			}

			if(openmrsProvider == null){
				openmrsProvider = new org.openmrs.Provider();
				openmrsProvider.setPerson(new Person());
				changed = true;
			}

			//Set user for
			if (provider != null){

				PersonName providerName = new PersonName(firstname, "", lastname);
				providerName.isPreferred();
				openmrsProvider.getPerson().addName(providerName);
				openmrsProvider.getPerson().setGender("U");
				openmrsProvider.setRetired(false);

				//Store the provider's id in the provider's person attribute.
				PersonAttribute pattr = new PersonAttribute();
				if (ps.getPersonAttributeTypeByName(PROVIDER_ID) != null&&
						provider.ehrProviderId!=null&&provider.ehrProviderId.length()>0){
					PersonAttribute attr = openmrsProvider.getPerson().getAttribute(
						ps.getPersonAttributeTypeByName(PROVIDER_ID));
					//only update if this is truly a new attribute value
					if (attr == null || !attr.getValue().equals(provider.ehrProviderId)) {
						pattr.setAttributeType(ps.getPersonAttributeTypeByName(PROVIDER_ID));
						pattr.setValue(provider.ehrProviderId);
						pattr.setCreator(Context.getAuthenticatedUser());
						pattr.setDateCreated(new Date());
						openmrsProvider.getPerson().addAttribute(pattr);
						changed = true;
					}
				}

				if(changed){
					savedProvider = providerService.saveProvider(openmrsProvider);
				}else{
					provider.setProviderId(openmrsProvider.getId());
					return openmrsProvider;
				}

				if (savedProvider != null  && savedProvider.getId() != null) {
					provider.providerId = savedProvider.getId();
				}
			}

		} catch (Exception e){
			log.error("Error while creating or updating a user for provider.", e);
		}



		return savedProvider;
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
	    PersonAttribute providerId = openmrsProvider.getPerson().getAttribute(PROVIDER_ID);
	    if (providerId == null) {
	    	setEhrProviderId("");
	    }
	    else{
	    	setEhrProviderId(providerId.getValue());
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
