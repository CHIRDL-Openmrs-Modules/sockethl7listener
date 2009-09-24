package org.openmrs.module.sockethl7listener.hibernateBeans;




/**
 * Hello World Saying
 * 
 * @author Ben Wolfe
 * @version 1.0
 */
public class NPI implements java.io.Serializable {


	private int Id;
	private String providerLastName = "";
	private String providerFirstName = "";
	private String npi = "";
	private String faxNumber = null;
	
	
	// Constructors
	
	/** default constructor */
	public NPI() {
	}
	
	
	public int getId() {
		return Id;
	}


	public void setId(int Id) {
		this.Id = Id;
	}


	public String getProviderFirstName() {
		return providerFirstName;
	}


	public void setProviderFirstName(String providerFirstName) {
		this.providerFirstName = providerFirstName;
	}

	/**
	 * @return the faxNumber
	 */
	public String getFaxNumber()
	{
		return this.faxNumber;
	}

	/**
	 * @param faxNumber the faxNumber to set
	 */
	public void setFaxNumber(String faxNumber)
	{
		this.faxNumber = faxNumber;
	}

	public String getNpi() {
		return npi;
	}


	public void setNpi(String npi) {
		this.npi = npi;
	}


	public String getProviderLastName() {
		return providerLastName;
	}


	public void setProviderLastName(String providerLastName) {
		this.providerLastName = providerLastName;
	}

	
	
}
