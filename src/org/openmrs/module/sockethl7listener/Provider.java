package org.openmrs.module.sockethl7listener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PersonService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;


/**
 * Encounter provider class specific to information from PV1 segment of hl7 messages.
 * @author msheley
 *
 */
public class Provider {

	private String firstName;
	private String lastName;
	private String id;
	private Integer providerId;
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
		id = "";
		
	}
	
	public Provider (String observationName){
		firstName = "";
		lastName = "";
		id = "";
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = org.openmrs.module.chirdlutil.util.Util.toProperCase(firstName);
		
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = org.openmrs.module.chirdlutil.util.Util.toProperCase(lastName);
	}
	
	/**
	 * @return providerId
	 */
	public Integer getProviderId(){
		return this.providerId;
	}
	
	/**
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
			
			//set the username
			String username = "";
			String firstname = provider.getFirstName();
			String lastname = provider.getLastName();
			String providerId = provider.getId();
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
				
			// TODO CHICA-221 Remove later
//			username = fn + "." + ln + "." + userid;
//			providerUser.setUsername(username);
//
//			//get existing provider or create password if no provider exists.
//			List<User> providers = us.getUsers(username, roles, true);
//			if (providers != null && providers.size()> 0){
//				providerUser = providers.get(0);
//				
//			} else{
//				UUID uuid = UUID.randomUUID();
//				password = uuid.toString();
//				providerUser.setPerson(new Person());
//				changed = true;
//			}
//			
//			Role r = us.getRole("Provider");
//			roles.add(r);
//			if(!providerUser.hasRole(r.getRole())){
//				providerUser.addRole(r); 
//				changed = true;
//			}
			
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
						provider.id!=null&&provider.id.length()>0){
					PersonAttribute attr = openmrsProvider.getPerson().getAttribute(
						ps.getPersonAttributeTypeByName(PROVIDER_ID));
					//only update if this is truly a new attribute value
					if (attr == null || !attr.getValue().equals(provider.id)) {
						pattr.setAttributeType(ps.getPersonAttributeTypeByName(PROVIDER_ID));
						pattr.setValue(provider.id);
						pattr.setCreator(Context.getAuthenticatedUser());
						pattr.setDateCreated(new Date());
						openmrsProvider.getPerson().addAttribute(pattr);
						changed = true;
					}
				}

				// TODO CHICA-221 Are these attributes needed?
//				PersonAttribute posFacAttr = new PersonAttribute();
//				if (pocFacility != null && ps.getPersonAttributeTypeByName("POC_FACILITY") != null){
//					PersonAttribute attr = openmrsProvider.getPerson().getAttribute(ps.getPersonAttributeTypeByName("POC_FACILITY"));
//					//only update if this is truly a new attribute value
//					if (attr == null || !attr.getValue().equals(pocFacility)) {
//						posFacAttr.setAttributeType(ps.getPersonAttributeTypeByName("POC_FACILITY"));
//						posFacAttr.setValue(pocFacility);
//						posFacAttr.setCreator(Context.getAuthenticatedUser());
//						posFacAttr.setDateCreated(new Date());
//						openmrsProvider.getPerson().addAttribute(posFacAttr);
//						changed = true;
//					}
//				}
//
//				PersonAttribute posBedAttr = new PersonAttribute();
//				if (pocBed != null && ps.getPersonAttributeTypeByName("POC_BED") != null){
//					PersonAttribute attr = providerUser.getPerson().getAttribute(
//						ps.getPersonAttributeTypeByName("POC_BED"));
//					//only update if this is truly a new attribute value
//					if (attr == null || !attr.getValue().equals(pocBed)) {
//						posBedAttr.setAttributeType(ps.getPersonAttributeTypeByName("POC_BED"));
//						posBedAttr.setValue(pocBed);
//						posBedAttr.setCreator(Context.getAuthenticatedUser());
//						posBedAttr.setDateCreated(new Date());
//						providerUser.getPerson().addAttribute(posBedAttr);
//						changed = true;
//					}
//				}
//
//				PersonAttribute posAttr = new PersonAttribute();
//				if (poc != null && ps.getPersonAttributeTypeByName("POC") != null) {
//					PersonAttribute attr = providerUser.getPerson().getAttribute(ps.getPersonAttributeTypeByName("POC"));
//					//only update if this is truly a new attribute value
//					if (attr == null || !attr.getValue().equals(poc)) {
//						posAttr.setAttributeType(ps.getPersonAttributeTypeByName("POC"));
//						posAttr.setValue(poc);
//						posAttr.setCreator(Context.getAuthenticatedUser());
//						posAttr.setDateCreated(new Date());
//						providerUser.getPerson().addAttribute(posAttr);
//						changed = true;
//					}
//				}
//
//				PersonAttribute posRoomAttr = new PersonAttribute();
//				if (pocRoom != null && ps.getPersonAttributeTypeByName("POC_ROOM") != null) {
//					PersonAttribute attr = providerUser.getPerson().getAttribute(ps.getPersonAttributeTypeByName("POC_ROOM"));
//					//only update if this is truly a new attribute value
//					if (attr == null || !attr.getValue().equals(pocRoom)) {
//						posRoomAttr.setAttributeType(ps.getPersonAttributeTypeByName("POC_ROOM"));
//						posRoomAttr.setValue(pocRoom);
//						posRoomAttr.setCreator(Context.getAuthenticatedUser());
//						posRoomAttr.setDateCreated(new Date());
//						providerUser.getPerson().addAttribute(posRoomAttr);
//						changed = true;
//					}
//				}
//
//
//				PersonAttribute adminSourceAttr = new PersonAttribute();
//				if (admitSource != null && ps.getPersonAttributeTypeByName("ADMIT_SOURCE") != null) {
//					PersonAttribute attr = providerUser.getPerson().getAttribute(ps.getPersonAttributeTypeByName("ADMIT_SOURCE"));
//					//only update if this is truly a new attribute value
//					if (attr == null || !attr.getValue().equals(admitSource)) {
//						adminSourceAttr.setAttributeType(ps.getPersonAttributeTypeByName("ADMIT_SOURCE"));
//						adminSourceAttr.setValue(admitSource);
//						adminSourceAttr.setCreator(Context.getAuthenticatedUser());
//						adminSourceAttr.setDateCreated(new Date());
//						providerUser.getPerson().addAttribute(adminSourceAttr);
//						changed = true;
//					}
//				}

				if(changed){
					// TODO CHICA-221 Remove if this is no longer needed
//					if (password != null) {
//						// Password has to be a mix of upper and lower case.
//						int passLength = password.length();
//						String firstPass = password.substring(0, passLength/2);
//						String secondPass = password.substring(passLength/2, passLength);
//						password = firstPass.toUpperCase() + secondPass;
//					}
					savedProvider = providerService.saveProvider(openmrsProvider);
				}else{
					provider.setId(openmrsProvider.getId().toString());
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
	    	setId("");
	    }
	    else{
	    	setId(providerId.getValue());
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
	
	// TODO CHICA-221 Remove later. This method doesn't appear to be used
//	public User initializeProvider(User existingProv){
//		User provider = new User();
//		if (existingProv != null){
//			provider.setUserId(existingProv.getUserId());
//			provider.getPerson().setAttributes(existingProv.getPerson().getAttributes());
//			provider.setDateRetired(existingProv.getDateRetired());
//			provider.setRetired(false);
//			provider.setCreator(existingProv.getCreator());
//			provider.setDateCreated(existingProv.getDateCreated());
//			provider.getPerson().setNames(existingProv.getPerson().getNames());
//			provider.getPerson().setGender(existingProv.getPerson().getGender());
//			provider.setId(existingProv.getId());
//		}
//		return provider;
//	}

}
