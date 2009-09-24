package org.openmrs.module.sockethl7listener.hibernateBeans;




/**
 * Hello World Saying
 * 
 * @author Ben Wolfe
 * @version 1.0
 */
public class InstProv implements java.io.Serializable {


	private int Id;
	private String providerLastName = "";
	private String providerFirstName = "";
	private String npi = "";

	
	// Constructors
	
	/** default constructor */
	public InstProv() {
	}
	
	
	public int getInstId() {
		return Id;
	}


	public void setInstId(int instId) {
		this.Id = instId;
	}


	public String getProviderFirstName() {
		return providerFirstName;
	}


	public void setProviderFirstName(String providerFirstName) {
		this.providerFirstName = providerFirstName;
	}


	public String getProviderID() {
		return npi;
	}


	public void setProviderID(String providerID) {
		this.npi = providerID;
	}


	public String getProviderLastName() {
		return providerLastName;
	}


	public void setProviderLastName(String providerLastName) {
		this.providerLastName = providerLastName;
	}

	
	
}
