/**
 * 
 */
package org.openmrs.module.sockethl7listener.hibernateBeans;

import java.util.Date;

/**
 * @author msheley
 *
 */
public class PatientMessage {

	private Integer message_id;
	private Boolean duplicateString = false;
	private Boolean duplicateDatetime = false;
	private Date dateCreated = new Date();
	private Integer patient_id;
	private Integer encounter_id;
	private String hl7Message = "";
	private String md5 = null;
	private String hl7source = "";
	
	public Integer getMessage_id() {
		return message_id;
	}
	public void setMessage_id(Integer message_id) {
		this.message_id = message_id;
	}
	
	public Integer getPatient_id() {
		return patient_id;
	}
	public void setPatient_id(Integer patient_id) {
		this.patient_id = patient_id;
	}
	public Integer getEncounter_id() {
		return encounter_id;
	}
	public void setEncounter_id(Integer encounter_id) {
		this.encounter_id = encounter_id;
	}
	public String getHl7Message() {
		return hl7Message;
	}
	public void setHl7Message(String hl7Message) {
		this.hl7Message = hl7Message;
	}
	public Boolean getDuplicateString() {
		return duplicateString;
	}
	public void setDuplicateString(Boolean duplicateString) {
		this.duplicateString = duplicateString;
	}
	public Boolean getDuplicateDatetime() {
		return duplicateDatetime;
	}
	public void setDuplicateDatetime(Boolean duplicateDatetime) {
		this.duplicateDatetime = duplicateDatetime;
	}
	public Date getDateCreated() {
		return dateCreated;
	}
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	public String getMd5()
	{
		return this.md5;
	}
	public void setMd5(String md5)
	{
		this.md5 = md5;
	}
	public void setHl7source(String hl7source) {
		this.hl7source = hl7source;
	}
	public String getHl7source() {
		return hl7source;
	}
}
