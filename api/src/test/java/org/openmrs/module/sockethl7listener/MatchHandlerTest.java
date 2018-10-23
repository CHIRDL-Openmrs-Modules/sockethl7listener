package org.openmrs.module.sockethl7listener;


import java.util.Calendar;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.api.context.Context;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.Verifies;

public class MatchHandlerTest extends BaseModuleContextSensitiveTest {
	/**
	 * @see {@link MatchHandler#getBestName(Patient,Patient,Date)}
	 * 
	 */
	@Test
	@Verifies(value = "should return most recent valid name", method = "getBestName(PersonName,PersonName,Date)")
	public void getBestName_shouldReturnMostRecentValidName() throws Exception 
	{
				Context.openSession();
				try {
					PersonName nameNew = new PersonName("Jerry", "Joseph" ,"Smith");
					Calendar cal1 = Calendar.getInstance();
				    //Clear all fields
				    cal1.clear();
				    cal1.set(Calendar.YEAR, 2008);
				    cal1.set(Calendar.MONTH, 3);
				    cal1.set(Calendar.DATE, 4);
				    Date encounterdate = cal1.getTime();
				    
					
					
					PersonName nameExists = new PersonName("John", "David" ,"Jones");
					Calendar cal2 = Calendar.getInstance();
				    //Clear all fields
					cal2.clear();
					cal2.set(Calendar.YEAR, 2008);
					cal2.set(Calendar.MONTH, 3);
					cal2.set(Calendar.DATE, 3);
				    Date date2 = cal2.getTime();
				    nameExists.setDateCreated(date2);
					MatchHandler mh = new MatchHandler();
					PersonName resultName = mh.getBestName(nameNew, nameExists, encounterdate);
					Assert.assertNotNull(resultName);
					Assert.assertEquals(resultName.getGivenName(), "Jerry") ;
					Assert.assertEquals(resultName.getMiddleName(), "Joseph") ;
					Assert.assertEquals(resultName.getFamilyName(), "Smith") ;
				} catch(Exception e) {
					Assert.fail(e.getMessage());
				} finally {
					Context.closeSession();
				}
			

			return;
			
				
	}

	/**
	 * @see {@link MatchHandler#getBestName(PersonName,PersonName,Date)}
	 * 
	 */
	@Test
	@Verifies(value = "should return name with valid format even if valid is older", method = "getBestName(PersonName,PersonName,Date)")
	public void getBestName_shouldReturnNameWithValidFormatEvenIfValidIsOlder()
			throws Exception {
		Context.openSession();
		try {
			PersonName nameNew = new PersonName("infant", "Joseph" ,"Smith");
			Calendar cal1 = Calendar.getInstance();
		    //Clear all fields
		    cal1.clear();
		    cal1.set(Calendar.YEAR, 2008);
		    cal1.set(Calendar.MONTH, 3);
		    cal1.set(Calendar.DATE, 4);
		    Date encounterdate = cal1.getTime();
		    
			
			
			PersonName nameExists = new PersonName("John", "David" ,"Jones");
			Calendar cal2 = Calendar.getInstance();
		    //Clear all fields
			cal2.clear();
			cal2.set(Calendar.YEAR, 2008);
			cal2.set(Calendar.MONTH, 3);
			cal2.set(Calendar.DATE, 3);
		    Date date2 = cal2.getTime();
		    nameExists.setDateCreated(date2);
			MatchHandler mh = new MatchHandler();
			PersonName resultName = mh.getBestName(nameNew, nameExists, encounterdate);
			Assert.assertNotNull(resultName);
			Assert.assertEquals(resultName.getGivenName(), "John") ;
			Assert.assertEquals(resultName.getMiddleName(), "Joseph") ;
			Assert.assertEquals(resultName.getFamilyName(), "Smith") ;
			Assert.assertEquals(resultName.getPreferred(), true);
		} catch(Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			Context.closeSession();
		}
		
		

	return;
	}

	/**
	 * @see {@link MatchHandler#getBestName(PersonName,PersonName,Date)}
	 * 
	 */
	@Test
	@Verifies(value = "return nonnull fn ln mn even if null is newer", method = "getBestName(PersonName,PersonName,Date)")
	public void getBestName_shouldReturnNonnullFnLnMnEvenIfNullIsNewer()
			throws Exception {
		Context.openSession();
		try {
			PersonName nameNew = new PersonName(null, "Jack" ,"Smith");
			Calendar cal1 = Calendar.getInstance();
		    //Clear all fields
		    cal1.clear();
		    cal1.set(Calendar.YEAR, 2008);
		    cal1.set(Calendar.MONTH, 3);
		    cal1.set(Calendar.DATE, 4);
		    Date encounterdate = cal1.getTime();
		    
			
			
			PersonName nameExists = new PersonName("John", "James" ,"Jones");
			Calendar cal2 = Calendar.getInstance();
		    //Clear all fields
			cal2.clear();
			cal2.set(Calendar.YEAR, 2008);
			cal2.set(Calendar.MONTH, 3);
			cal2.set(Calendar.DATE, 3);
		    Date date2 = cal2.getTime();
		    nameExists.setDateCreated(date2);
			MatchHandler mh = new MatchHandler();
			PersonName resultName = mh.getBestName(nameNew, nameExists, encounterdate);
			Assert.assertNotNull(resultName);
			Assert.assertEquals(resultName.getGivenName(), "John") ;
			Assert.assertEquals(resultName.getMiddleName(), "Jack") ;
			Assert.assertEquals(resultName.getFamilyName(), "Smith") ;
			Assert.assertEquals(resultName.getPreferred(), true);
			Assert.assertTrue(resultName.getDateCreated().equals(encounterdate));
		} catch(Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			Context.closeSession();
		}
		
		
	}

	/**
	 * @see {@link MatchHandler#getBestName(PersonName,PersonName,Date)}
	 * 
	 */
	@Test
	@Verifies(value = "should return notnull PersonName if one PersonName is null", method = "getBestName(PersonName,PersonName,Date)")
	public void getBestName_shouldReturnNotnullPersonNameIfOnePersonNameIsNull()
			throws Exception {
		Context.openSession();
		try {
			PersonName nameNew = null;
			Calendar cal1 = Calendar.getInstance();
		    //Clear all fields
		    cal1.clear();
		    cal1.set(Calendar.YEAR, 2008);
		    cal1.set(Calendar.MONTH, 3);
		    cal1.set(Calendar.DATE, 4);
		    Date encounterdate = cal1.getTime();
		
			PersonName nameExists = new PersonName("John", "James" ,"Jones");
			Calendar cal2 = Calendar.getInstance();
		    cal2.clear();
			cal2.set(Calendar.YEAR, 2008);
			cal2.set(Calendar.MONTH, 3);
			cal2.set(Calendar.DATE, 3);
		    Date date2 = cal2.getTime();
		    nameExists.setDateCreated(date2);
			MatchHandler mh = new MatchHandler();
			PersonName resultName = mh.getBestName(nameNew, nameExists, encounterdate);
			Assert.assertNotNull(resultName);
			Assert.assertEquals(resultName.getGivenName(), "John") ;
			Assert.assertEquals(resultName.getMiddleName(), "James") ;
			Assert.assertEquals(resultName.getFamilyName(), "Jones") ;
			Assert.assertEquals(resultName.getPreferred(), true);
			Assert.assertTrue(resultName.getDateCreated().equals(date2));
		} catch(Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			Context.closeSession();
		}
	}

	/**
	 * @see {@link MatchHandler#getBestTel(PersonAttribute,PersonAttribute,Date)}
	 * 
	 */
	@Test
	@Verifies(value = "should return newer attribute", method = "getBestTel(PersonAttribute,PersonAttribute,Date)")
	public void getBestTel_shouldReturnNewerAttribute() throws Exception {
		Context.openSession();
		try {
			PersonAttributeType pat = new PersonAttributeType();
			pat.setName("Telephone Number");
			
			Calendar cal = Calendar.getInstance();
		    cal.clear();
			cal.set(Calendar.YEAR, 2008);
			cal.set(Calendar.MONTH, 3);
			cal.set(Calendar.DATE, 3);
		    Date date = cal.getTime();
			PersonAttribute newAttr = new PersonAttribute(pat,"1234567890");
			newAttr.setDateCreated(date);
			newAttr.setVoided(false);
			
			
			Calendar cal2 = Calendar.getInstance();
		    cal2.clear();
			cal2.set(Calendar.YEAR, 2007);
			cal2.set(Calendar.MONTH, 4);
			cal2.set(Calendar.DATE, 2);
		    Date date2 = cal2.getTime();
			PersonAttribute oldAttr = new PersonAttribute(pat,"222222222");
			oldAttr.setDateCreated(date2);
			oldAttr.setVoided(false);
			
			MatchHandler mh = new MatchHandler();
			PersonAttribute result = mh.getBestTel(newAttr, oldAttr, date);
			Assert.assertEquals(result.getValue(), "1234567890");
			Assert.assertTrue(result.getDateCreated().equals(date));
		} catch(Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			Context.closeSession();
		}
		
	}

	/**
	 * @see {@link MatchHandler#getBestTel(PersonAttribute,PersonAttribute,Date)}
	 * 
	 */
	@Test
	@Verifies(value = "should return non null attribute", method = "getBestTel(PersonAttribute,PersonAttribute,Date)")
	public void getBestTel_shouldReturnNonNullAttribute() throws Exception {
		Context.openSession();
		try {
			PersonAttributeType pat = new PersonAttributeType();
			pat.setName("Telephone Number");
			
			Calendar cal = Calendar.getInstance();
		    cal.clear();
			cal.set(Calendar.YEAR, 2008);
			cal.set(Calendar.MONTH, 3);
			cal.set(Calendar.DATE, 3);
		    Date date = cal.getTime();
			PersonAttribute newAttr = null;
			
			
			Calendar cal2 = Calendar.getInstance();
		    cal2.clear();
			cal2.set(Calendar.YEAR, 2007);
			cal2.set(Calendar.MONTH, 4);
			cal2.set(Calendar.DATE, 2);
		    Date date2 = cal2.getTime();
			PersonAttribute oldAttr = new PersonAttribute(pat,"222222222");
			oldAttr.setDateCreated(date2);
			oldAttr.setVoided(false);
			
			MatchHandler mh = new MatchHandler();
			PersonAttribute result = mh.getBestTel(newAttr, oldAttr, date);
			Assert.assertEquals(result.getValue(), "222222222");
			Assert.assertTrue(result.getDateCreated().equals(date2));
		} catch(Exception e) {
			Assert.fail(e.getMessage());
		} finally {
			Context.closeSession();
		}
		
	}
		
}
