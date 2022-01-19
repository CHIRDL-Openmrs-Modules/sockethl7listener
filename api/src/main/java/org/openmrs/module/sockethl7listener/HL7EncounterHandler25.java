/**
 * 
 */
package org.openmrs.module.sockethl7listener;

import java.util.Date;

import org.openmrs.PersonName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.datatype.CX;
import ca.uhn.hl7v2.model.v25.datatype.HD;
import ca.uhn.hl7v2.model.v25.datatype.IS;
import ca.uhn.hl7v2.model.v25.datatype.PL;
import ca.uhn.hl7v2.model.v25.datatype.TS;
import ca.uhn.hl7v2.model.v25.datatype.XCN;
import ca.uhn.hl7v2.model.v25.message.ADT_A01;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.segment.PV1;
import ca.uhn.hl7v2.model.v25.segment.PV2;

/**
 * @author tmdugan
 * 
 */
public class HL7EncounterHandler25 implements HL7EncounterHandler
{
	private static final Logger log =  LoggerFactory.getLogger("SocketHandlerLogger");

	public Provider getProvider(Message message)
	{
		Provider provider = new Provider();
		XCN doctor = null;
		PV1 pv1 = getPV1(message);
		try
		{
			doctor = pv1.getAttendingDoctor(0);
			
			// Load provider object with PV1 information

			if (doctor != null)
			{
				PersonName name = getDoctorName(message);
				provider.setFirstName(name.getGivenName());
				provider.setLastName(name.getFamilyName());
				String id = "";
				if (doctor.getIDNumber() != null)
				{
					id = doctor.getIDNumber().toString();
				}
				provider.setEhrProviderId(id);

			}
			
			PL patientLoc  = pv1.getAssignedPatientLocation();
			if (patientLoc != null){
				HD fac = patientLoc.getFacility();
				if (fac != null && fac.getNamespaceID()!= null){
					provider.setPocFacility(fac.getNamespaceID().getValue());
				}
				IS poc = patientLoc.getPointOfCare();
				if (poc != null){
					provider.setPoc(poc.getValue());
				}
				IS bed = patientLoc.getBed();
				if (bed != null){
					provider.setPocBed(bed.getValue());
				}
				IS room = patientLoc.getRoom();
				if (room != null){
					provider.setPocRoom(room.getValue());
				}
				
			}
			
			IS admitSource = pv1.getAdmitSource();
			if (admitSource != null){
				provider.setAdmitSource(admitSource.getValue());
			}
			
			return provider;

		} catch (RuntimeException e){
			log.error("Error creating provider from hl7 message",e) ;
		}
		return null;
	}

	protected PersonName getDoctorName(Message message)
	{
		PersonName name = new PersonName();
		XCN doctor = null;
		PV1 pv1 = getPV1(message);
		try
		{
			doctor = pv1.getAttendingDoctor(0);
		} catch (Exception e)
		{
			log.error("Unable to parse doctor name from PV1 segment.", e);
		}

		if (doctor != null)
		{
			String firstName = "";
			String lastName = "";

			if (doctor.getGivenName() != null)
			{
				firstName = doctor.getGivenName().toString();
				name.setGivenName(firstName);
			}
			if (doctor.getFamilyName() != null)
			{
				lastName = doctor.getFamilyName().getSurname().toString();
				name.setFamilyName(lastName);
			}

		}
		return name;
	}

	/* (non-Javadoc)
	 * @see org.openmrs.module.sockethl7listener.HL7EncounterHandler#getEncounterDate(ca.uhn.hl7v2.model.Message)
	 */
	public Date getEncounterDate(Message message) {
		TS timeStamp = null;
		Date datetime = null;

		try {
			PV1 pv1 = getPV1(message);
			MSH msh = getMSH(message);
			OBR obr = getOBR(message, 0);
			timeStamp = null;
			
			if (message instanceof ORU_R01) {
				if (obr != null)
					timeStamp = obr.getObservationDateTime();
			} else if ((message instanceof ADT_A01)) {
				if (pv1 != null)
					timeStamp = pv1.getAdmitDateTime();
			}
			 if (timeStamp == null || timeStamp.getTime()== null || timeStamp.getTime().getValue() == null){
				 if (msh != null){
					 timeStamp = msh.getDateTimeOfMessage();
			 	}
			 }
			
			if (timeStamp != null && timeStamp.getTime()!= null) { 
				datetime = TranslateDate(timeStamp);
			}else {
				log.error("A valid encounter date time stamp  not found in PV1 (ADT messages), OBR (ORU messages), or MSH segments.");
			}
			
		} catch (Exception e) {
			log.error("Exception getting encounter date from hl7 message.", e);
		}

		return datetime;

	}

	protected PV1 getPV1(Message message)
	{
		if (message instanceof ORU_R01)
		{
			return getPV1((ORU_R01) message);
		}
		if (message instanceof ADT_A01)
		{
			return getPV1((ADT_A01) message);
		}
		return null;
	}

	private PV1 getPV1(ORU_R01 oru)
	{
		return oru.getPATIENT_RESULT().getPATIENT().getVISIT().getPV1();
	}

	private PV1 getPV1(ADT_A01 adt)
	{
		return adt.getPV1();
	}

	protected OBR getOBR(Message message, int orderRep)
	{
		return HL7ObsHandler25.getOBR(message, orderRep);
	}

	protected MSH getMSH(Message message)
	{
		return HL7ObsHandler25.getMSH(message);
	}

	protected Date TranslateDate(TS ts)
	{
		return HL7ObsHandler25.TranslateDate(ts);
	}
	
	/**
	 * DWE CHICA-633 
	 * Get visit number from PV1-19
	 */
	@Override
	public String getVisitNumber(Message message)
	{
		CX visitNumber = null;
		PV1 pv1 = getPV1(message);
		try
		{
			visitNumber = pv1.getVisitNumber();
		} 
		catch (RuntimeException e)
		{
			log.error("Unable to parse visit number from PV1-19.", e);
		}

		if (visitNumber != null)
		{
			try
			{
				return visitNumber.getIDNumber().toString();
			} 
			catch (RuntimeException e)
			{
				log.error("Unable to parse visit number id from PV1-19 visit number field. ", e);
			}
		}
		return null;	
	}
	
	/**
	 * DWE CHICA-751
	 * Get location description from PV1-3.9
	 * Note: Mirth is being used to copy the original value received
	 * in PV1-3.1 to the location description field (PV1-3.9)
	 */
	public String getLocationDescription(Message message)
	{
		PV1 pv1 = getPV1(message);
		try
		{
			return pv1.getAssignedPatientLocation().getLocationDescription().getValue();
		} 
		catch (RuntimeException e)
		{
			log.error("Unable to parse original location from PV1-3.9. ", e);
		}
		return null;	
	}
	
	/**
	 * CHICA-982
	 * Get location from PV1-3.1
	 */
	public String getLocation(Message message)
	{
		PV1 pv1 = getPV1(message);
		return pv1.getAssignedPatientLocation().getPointOfCare().getValue();
	}
	
	/**
	 * CHICA-1160
	 * Get PV2 segment
	 * @param message
	 * @return
	 */
	private PV2 getPV2(Message message)
	{
		if (message instanceof ADT_A01)
		{
			return getPV2((ADT_A01) message);
		}
		return null;
	}

	/**
	 * CHICA-1160
	 * Get PV2 segment from ADT message
	 * @param adt
	 * @return
	 */
	private PV2 getPV2(ADT_A01 adt)
	{
		return adt.getPV2();
	}
	
	/**
	 * CHICA-1160
	 * Get visit type (visit description) from PV2-12
	 */
	public String getVisitType(Message message)
	{
		PV2 pv2 = getPV2(message);
		if(pv2 != null)
		{
			return pv2.getVisitDescription().getValue();
		}
		return null;
	}
}
