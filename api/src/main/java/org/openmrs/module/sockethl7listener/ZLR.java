package org.openmrs.module.sockethl7listener;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;


public class ZLR   {
    
	private Message message;
	private String messageString;
	private String zlrString;
	private String orderingProviderAddressString;
	private String orderingProviderCity;
	private String orderingProviderState;
	private String orderingProviderZip;
	private String orderingProviderStreetAddress;
	private String orderingProviderOtherDesignation;
	
	
	private String orderingFacilityNameString;
	private String orderingFacilityOrganizationName;
	private String orderingFacilityTypeCode;
	private String orderingFacilityIDNum;
	
	
    
    public ZLR (Message message) {
    	
    	this.message = message;
    	
    	orderingProviderAddressString = "";
    	orderingProviderOtherDesignation = "";
    	orderingProviderCity = "";
    	orderingProviderState = "";
    	orderingProviderZip = "";
    	orderingProviderStreetAddress = "";
    	orderingProviderOtherDesignation = "";
  
    	orderingFacilityNameString = "";
    	orderingFacilityTypeCode = "";
    	orderingFacilityOrganizationName = "";
    	orderingFacilityIDNum = "";
    	
    }

	/**
	 * Parses the zlr segment string into fields
	 * @param zlrString
	 */
	public void parseFields (String zlrString){
		 
		String [] fields = PipeParser.split(zlrString, "|");
		if (fields != null) {
			int length = fields.length;
			this.orderingProviderAddressString = (length >= 2 ) ? fields[1] : "";
			this.orderingFacilityNameString = (length >= 3 ) ? fields[2] : "";
			
		}
	}
	
	/**
	 * Gets the ZLR segment string from the incoming hl7 message string
	 * @param mstring
	 * @return
	 */
	private String getZLRSegmentString(String mstring){
		String ret = "";

       String [] segments = PipeParser.split(mstring, "\r");
        for (String s : segments){
        	if (s != null && s.startsWith("ZLR")){
        		ret = s;
        	}
        }
        
		return ret;
	}
	
	/**
	 * Parses the ZLR segment string from the incoming message string, 
	 * and inserts the information as ZLR segment into message object.
	 * @param mstring
	 */
	public void loadZLRSegment(String mstring){
		try {
			parseFields(getZLRSegmentString(mstring));
			Terser terser = new Terser(message);
			terser.set("ZLR-1", orderingProviderAddressString);
			terser.set("ZLR-2", orderingFacilityNameString);
		} catch (HL7Exception e) {
			e.printStackTrace();
		}
	}
	
	

	/**
	 * Gets  field 2 from ZLR segment and sets Facility ID number from the third component.
	 * @return
	 */
	public String getOrderingFacilityIDNum() {
		try {
				
				Terser t = new Terser(message);
				String [] components = PipeParser.split(t.get("ZLR-2"), "^");
				
				if (components != null) {
					this.orderingFacilityIDNum = (components.length >= 3 ) ? components[2] : "";
				}
			} catch (HL7Exception e) {
				e.printStackTrace();
			}
			
		
		return orderingFacilityIDNum;
	}

	
	public String getOrderingFacilityOrganizationName() {
		try {
			
			Terser t = new Terser(message);
			String [] components = PipeParser.split(t.get("ZLR-2"), "^");
			
			if (components != null) {
				this.orderingFacilityOrganizationName = (components.length >= 1 ) ? components[0] : "";
			}
		} catch (HL7Exception e) {
			e.printStackTrace();
		}
		return orderingFacilityOrganizationName;
	}

	
	public String getOrderingFacilityTypeCode() {
		try {

			Terser t = new Terser(message);
			String [] components = PipeParser.split(t.get("ZLR-2"), "^");

			if (components != null) {
				this.orderingFacilityTypeCode = (components.length >= 2 ) ? components[1] : "";
			}
		} catch (HL7Exception e) {
			e.printStackTrace();
		}
		return orderingFacilityTypeCode;
	}

	public String getOrderingProviderCity() {
		try {
			
			Terser t = new Terser(message);
			String [] components = PipeParser.split(t.get("ZLR-1"), "^");
			
			if (components != null) {
				this.orderingProviderCity = (components.length >= 3 ) ? components[2] : "";
			}
		} catch (HL7Exception e) {
			e.printStackTrace();
		}
		
		return orderingProviderCity;
	}

	private void setOrderingProviderCity(String orderingProviderCity) {
		this.orderingProviderCity = orderingProviderCity;
	}

	public String getOrderingProviderState() {
		try {
			
			Terser t = new Terser(message);
			String [] components = PipeParser.split(t.get("ZLR-1"), "^");
			
			if (components != null) {
				this.orderingProviderState = (components.length >= 4 ) ? components[3] : "";
			}
		} catch (HL7Exception e) {
			e.printStackTrace();
		}
		return orderingProviderState;
	}

	
	public String getOrderingProviderStreetAddress() {
		try {
			
			Terser t = new Terser(message);
			String [] components = PipeParser.split(t.get("ZLR-1"), "^");
			
			if (components != null) {
				this.orderingProviderStreetAddress = (components.length >= 1 ) ? components[0] : "";
			}
		} catch (HL7Exception e) {
			e.printStackTrace();
		}
		return orderingProviderStreetAddress;
	}


	public String getOrderingProviderZip() {
		try {
			
			Terser t = new Terser(message);
			String [] components = PipeParser.split(t.get("ZLR-1"), "^");
			
			if (components != null) {
				this.orderingProviderZip = (components.length >= 5 ) ? components[4] : "";
			}
		} catch (HL7Exception e) {
			e.printStackTrace();
		}
		return orderingProviderZip;
	}

	public String getOrderingProviderOtherDesignation() {
		return orderingProviderOtherDesignation;
	}

	private void setOrderingProviderOtherDesignation(
			String orderingProviderOtherDesignation) {
		this.orderingProviderOtherDesignation = orderingProviderOtherDesignation;
	}

	private void setOrderingProviderState() {
		try {
			
			Terser t = new Terser(message);
			String [] components = PipeParser.split(t.get("ZLR-1"), "^");
			
			if (components != null) {
				this.orderingProviderState = (components.length >= 4 ) ? components[3] : "";
			}
		} catch (HL7Exception e) {
			e.printStackTrace();
		}
	}

	private void setOrderingProviderStreetAddress(String orderingProviderStreetAddress) {
		this.orderingProviderStreetAddress = orderingProviderStreetAddress;
	}

	private void setOrderingProviderZip(String orderingProviderZip) {
		this.orderingProviderZip = orderingProviderZip;
	}


}
    
    
	

   /**
   * Returns Ordering Provider Address (ZLR-1)
   */
  /**/

/*public class ZLR extends GenericSegment{
	private XAD orderingProvidersAddress;
	private XON orderingFacilityName;
	private XAD orderingFacilityAddress;
	private XTN orderingFacilityPhone;

	public ZLR(Group parent, ModelClassFactory factory ) {
		super(parent, "ZLR");
		Message message = getMessage();
		try {
			this.add(ST.class, true, 1, 1, new Object[]{message});
		} catch (HL7Exception he) {
			HapiLogFactory.getHapiLog(this.getClass()).error("Can't instantiate " + this.getClass().getName(), he);
		}
	}

	/**
	 * Returns File Field Separator (FHS-1).
	 */
	/*public ST getFileFieldSeparator()  {
		ST ret = null;
		try {
			Type t = this.getField(1, 0);
			ret = (ST)t;
		} catch (ClassCastException cce) {
			HapiLogFactory.getHapiLog(this.getClass()).error("Unexpected problem obtaining field value.  This is a bug.", cce);
			throw new RuntimeException(cce);
		} catch (HL7Exception he) {
			HapiLogFactory.getHapiLog(this.getClass()).error("Unexpected problem obtaining field value.  This is a bug.", he);
			throw new RuntimeException(he);
		}
		return ret;
	}

	public ST getTestInfo()  {
		ST ret = null;
		try {
			Type t = this.getField(1, 0);
			ret = (ST)t;
		} catch (ClassCastException cce) {
			HapiLogFactory.getHapiLog(this.getClass()).error("Unexpected problem obtaining field value.  This is a bug.", cce);
			throw new RuntimeException(cce);
		} catch (HL7Exception he) {
			HapiLogFactory.getHapiLog(this.getClass()).error("Unexpected problem obtaining field value.  This is a bug.", he);
			throw new RuntimeException(he);
		}
		return ret;
	}

	public XAD getOrderingFacilityAddress() {
		return orderingFacilityAddress;
	}

	public void setOrderingFacilityAddress(XAD orderingFacilityAddress) {
		this.orderingFacilityAddress = orderingFacilityAddress;
	}

	public XON getOrderingFacilityName() {
		return orderingFacilityName;
	}

	public void setOrderingFacilityName(XON orderingFacilityName) {
		this.orderingFacilityName = orderingFacilityName;
	}

	public XTN getOrderingFacilityPhone() {
		return orderingFacilityPhone;
	}

	public void setOrderingFacilityPhone(XTN orderingFacilityPhone) {
		this.orderingFacilityPhone = orderingFacilityPhone;
	}

	public XAD getOrderingProvidersAddress() {
		return orderingProvidersAddress;
	}

	public void setOrderingProvidersAddress(XAD orderingProvidersAddress) {
		this.orderingProvidersAddress = orderingProvidersAddress;
	}


}*/
