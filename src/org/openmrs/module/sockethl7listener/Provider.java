package org.openmrs.module.sockethl7listener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.PersonService;
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
	private Integer userid;
	private String poc;
	private String pocFacility;
	private String pocRoom;
	private String pocBed;
	private String admitSource;
	public UserService us;
	public AdministrationService as;
	public PersonService ps;
	private static final String PROVIDER_ID = "Provider ID";
	private Log log =  LogFactory.getLog(this.getClass());
	
	
	public Provider (){
		firstName = "";
		lastName = "";
		id = "";
		as = Context.getAdministrationService();
		ps = Context.getPersonService();
		us = Context.getUserService();
	}
	
	public Provider (String observationName){
		firstName = "";
		lastName = "";
		id = "";
		us = Context.getUserService();
		
		as = Context.getAdministrationService();
		ps = Context.getPersonService();
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
	public Integer getUserId (){
		return userid;
	}
	
	public void setUserId(Integer userid){
		this.userid = userid;
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
	
	
	/** CreateUserForProvider
	 * @param provider
	 * @return
	 */
	public User createUserForProvider(Provider provider)  {

		User providerUser = new User();
		List<Role> roles = new ArrayList <Role> ();
		User savedProviderUser = null;
		String password = null;
		boolean changed = false;
		PersonService personService = Context.getPersonService();

		try {
			
			//set the username
			String username = "";
			String firstname = provider.getFirstName();
			String lastname = provider.getLastName();
			String userid = provider.getId();
			String fn = "";
			String ln = "";
			
			if(firstname == null){
				firstname = "";
			}
			if(lastname == null){
				lastname = "";
			}
			if(userid == null){
				userid = "";
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
				
			
			username = fn + "." + ln + "." + userid;
			providerUser.setUsername(username);

			//get existing provider or create password if no provider exists.
			List<User> providers = us.getUsers(username, roles, true);
			if (providers != null && providers.size()> 0){
				providerUser = providers.get(0);
				
			} else{
				UUID uuid = UUID.randomUUID();
				password = uuid.toString();
				changed = true;
			}
			
			Role r = us.getRole("Provider");
			roles.add(r);
			if(!providerUser.hasRole(r.getRole())){
				providerUser.addRole(r); 
				changed = true;
			}

			//Set user for
			if (provider != null){

				PersonName providerName = new PersonName(firstname, "", lastname);
				providerName.isPreferred();
				providerUser.addName(providerName);
				providerUser.getPerson().setGender("U");
				providerUser.setRetired(false);

				//Store the provider's id in the provider's person attribute.
				PersonAttribute pattr = new PersonAttribute();
				if (ps.getPersonAttributeTypeByName(PROVIDER_ID) != null&&
						provider.id!=null&&provider.id.length()>0){
					PersonAttribute attr = providerUser.getPerson().getAttribute(
						ps.getPersonAttributeTypeByName(PROVIDER_ID));
					//only update if this is truly a new attribute value
					if (attr == null || !attr.getValue().equals(provider.id)) {
						pattr.setAttributeType(ps.getPersonAttributeTypeByName(PROVIDER_ID));
						pattr.setValue(provider.id);
						pattr.setCreator(Context.getAuthenticatedUser());
						pattr.setDateCreated(new Date());
						providerUser.getPerson().addAttribute(pattr);
						changed = true;
					}
				}


				PersonAttribute posFacAttr = new PersonAttribute();
				if (pocFacility != null && ps.getPersonAttributeTypeByName("POC_FACILITY") != null){
					PersonAttribute attr = providerUser.getPerson().getAttribute(ps.getPersonAttributeTypeByName("POC_FACILITY"));
					//only update if this is truly a new attribute value
					if (attr == null || !attr.getValue().equals(pocFacility)) {
						posFacAttr.setAttributeType(ps.getPersonAttributeTypeByName("POC_FACILITY"));
						posFacAttr.setValue(pocFacility);
						posFacAttr.setCreator(Context.getAuthenticatedUser());
						posFacAttr.setDateCreated(new Date());
						providerUser.getPerson().addAttribute(posFacAttr);
						changed = true;
					}
				}

				PersonAttribute posBedAttr = new PersonAttribute();
				if (pocBed != null && ps.getPersonAttributeTypeByName("POC_BED") != null){
					PersonAttribute attr = providerUser.getPerson().getAttribute(
						ps.getPersonAttributeTypeByName("POC_BED"));
					//only update if this is truly a new attribute value
					if (attr == null || !attr.getValue().equals(pocBed)) {
						posBedAttr.setAttributeType(ps.getPersonAttributeTypeByName("POC_BED"));
						posBedAttr.setValue(pocBed);
						posBedAttr.setCreator(Context.getAuthenticatedUser());
						posBedAttr.setDateCreated(new Date());
						providerUser.getPerson().addAttribute(posBedAttr);
						changed = true;
					}
				}

				PersonAttribute posAttr = new PersonAttribute();
				if (poc != null && ps.getPersonAttributeTypeByName("POC") != null) {
					PersonAttribute attr = providerUser.getPerson().getAttribute(ps.getPersonAttributeTypeByName("POC"));
					//only update if this is truly a new attribute value
					if (attr == null || !attr.getValue().equals(poc)) {
						posAttr.setAttributeType(ps.getPersonAttributeTypeByName("POC"));
						posAttr.setValue(poc);
						posAttr.setCreator(Context.getAuthenticatedUser());
						posAttr.setDateCreated(new Date());
						providerUser.getPerson().addAttribute(posAttr);
						changed = true;
					}
				}

				PersonAttribute posRoomAttr = new PersonAttribute();
				if (pocRoom != null && ps.getPersonAttributeTypeByName("POC_ROOM") != null) {
					PersonAttribute attr = providerUser.getPerson().getAttribute(ps.getPersonAttributeTypeByName("POC_ROOM"));
					//only update if this is truly a new attribute value
					if (attr == null || !attr.getValue().equals(pocRoom)) {
						posRoomAttr.setAttributeType(ps.getPersonAttributeTypeByName("POC_ROOM"));
						posRoomAttr.setValue(pocRoom);
						posRoomAttr.setCreator(Context.getAuthenticatedUser());
						posRoomAttr.setDateCreated(new Date());
						providerUser.getPerson().addAttribute(posRoomAttr);
						changed = true;
					}
				}


				PersonAttribute adminSourceAttr = new PersonAttribute();
				if (admitSource != null && ps.getPersonAttributeTypeByName("ADMIT_SOURCE") != null) {
					PersonAttribute attr = providerUser.getPerson().getAttribute(ps.getPersonAttributeTypeByName("ADMIT_SOURCE"));
					//only update if this is truly a new attribute value
					if (attr == null || !attr.getValue().equals(admitSource)) {
						adminSourceAttr.setAttributeType(ps.getPersonAttributeTypeByName("ADMIT_SOURCE"));
						adminSourceAttr.setValue(admitSource);
						adminSourceAttr.setCreator(Context.getAuthenticatedUser());
						adminSourceAttr.setDateCreated(new Date());
						providerUser.getPerson().addAttribute(adminSourceAttr);
						changed = true;
					}
				}

				if(changed){
					savedProviderUser = us.saveUser(providerUser, password);
				}else{
					provider.setUserId(providerUser.getUserId());
					return providerUser;
				}

				if (savedProviderUser != null  && savedProviderUser.getUserId() != null) {
					provider.userid = savedProviderUser.getUserId();
				}
			}

		} catch (Exception e){
			log.error("Error while creating or updating a user for provider.", e);
		}



		return savedProviderUser;
	}

	public User getUserForProvider(Provider provider) throws APIException {
	   
        Integer userid = provider.getUserId();
        User providerUser = us.getUser(userid);
        if ( providerUser == null){
        	providerUser = createUserForProvider(provider);
        }

		return providerUser;
	}
	
	


	public void setProviderfromUser(User provUser)  {
	
		if (provUser == null){
			provUser = createUserForProvider(this);
		}	
	    setFirstName(provUser.getGivenName());
	    setLastName(provUser.getFamilyName());
	    PersonAttribute providerId = provUser.getPerson().getAttribute(PROVIDER_ID);
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
	
	public User initializeProvider(User existingProv){
		User provider = new User();
		if (existingProv != null){
			provider.setUserId(existingProv.getUserId());
			provider.getPerson().setAttributes(existingProv.getPerson().getAttributes());
			provider.setDateRetired(existingProv.getDateRetired());
			provider.setRetired(false);
			provider.setCreator(existingProv.getCreator());
			provider.setDateCreated(existingProv.getDateCreated());
			provider.getPerson().setNames(existingProv.getPerson().getNames());
			provider.getPerson().setGender(existingProv.getPerson().getGender());
			provider.setId(existingProv.getId());
		}
		return provider;
	}

}
