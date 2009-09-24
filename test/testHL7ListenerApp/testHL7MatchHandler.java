package testHL7ListenerApp;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.PersonName;
import org.openmrs.module.sockethl7listener.MatchHandler;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;
import org.junit.Test;


@SkipBaseSetup
public class testHL7MatchHandler  extends BaseModuleContextSensitiveTest{

	@Before
	public void runBeforeEachTest() throws Exception
	{
		authenticate();
	}
	
	@Test
	public void testgetBestIdentifier(){
		Calendar PID = Calendar.getInstance();
		Calendar match = Calendar.getInstance();
	
		//PID newer
		Patient p = new Patient();
		PatientIdentifier PIDIdent = new PatientIdentifier();
		
		PID.set(2007, Calendar.JULY, 10);
		PIDIdent.setDateCreated(PID.getTime());
		PIDIdent.setIdentifier("1234");
		p.addIdentifier(PIDIdent);
		
		Patient matchedP = new Patient();
		PatientIdentifier matchedIdent = new PatientIdentifier();
		
		match.set(2007, Calendar.JUNE, 1);
		matchedIdent.setDateCreated(match.getTime());
		matchedIdent.setIdentifier("4321");
		matchedP.addIdentifier(matchedIdent);
		 
		MatchHandler mh = new MatchHandler();
		
		PatientIdentifier bestIdentifier = mh.getBestIdentifier(p, matchedP, PID.getTime());
		
		
		for (PatientIdentifier pi : matchedP.getIdentifiers()){
			System.out.println(pi.getIdentifier()+ " " + pi.getPreferred().toString()+ " " + pi.getDateCreated().toString());
		}
		assertEquals("1234",bestIdentifier.getIdentifier());
		assertEquals("true", bestIdentifier.getPreferred().toString());
		assertEquals(PIDIdent.getDateCreated(),bestIdentifier.getDateCreated());
		
		for (PatientIdentifier pi : matchedP.getIdentifiers()){
			System.out.println(pi.getIdentifier()+ " " + pi.getPreferred().toString()+ " " + pi.getDateCreated().toString());
		}
		
		//PID older
		
		
		Patient p2 = new Patient();
		PatientIdentifier PIDIdent2 = new PatientIdentifier();
		PID.set(2007, Calendar.MARCH, 10);
		PIDIdent2.setDateCreated(PID.getTime());
		PIDIdent2.setIdentifier("1234");
		p2.addIdentifier(PIDIdent2);   
		
		Patient matchedP2 = new Patient();
		PatientIdentifier matchedIdent2 = new PatientIdentifier();
		
		match.set(2007, Calendar.JUNE, 1);
		matchedIdent2.setDateCreated(match.getTime());
		matchedIdent2.setIdentifier("4321");
		matchedP2.addIdentifier(matchedIdent);
		
	    mh = new MatchHandler(); 
	    PatientIdentifier bestIdentifier2 = mh.getBestIdentifier(p2, matchedP2,PID.getTime());
	    
	    System.out.println("best Identifier:\n " + bestIdentifier2.getIdentifier()+ " " 
	    		+ bestIdentifier2.getPreferred().toString()+ " " 
	    		+ bestIdentifier2.getDateCreated().toString());
		
		for (PatientIdentifier pi : matchedP2.getIdentifiers()){
			System.out.println(pi.getIdentifier()+ " " + pi.getPreferred().toString()+ " " + pi.getDateCreated().toString());
		}
	    assertEquals("4321",bestIdentifier2.getIdentifier());
		assertEquals("true", bestIdentifier2.getPreferred().toString());
		assertEquals(matchedIdent2.getDateCreated(),bestIdentifier2.getDateCreated());
		
		//PID newer but null
		
		Patient p3 = new Patient();
		PatientIdentifier PIDIdent3 = new PatientIdentifier();
		PID.set(2007, Calendar.AUGUST, 10);
		PIDIdent3.setDateCreated(PID.getTime());
		PIDIdent3.setIdentifier(null);
		p3.addIdentifier(PIDIdent3);   
		 
	
		Patient matchedP3 = new Patient();
		match.set(2007, Calendar.JUNE, 1);
		PatientIdentifier matchedIdent3 = new PatientIdentifier();
	    matchedIdent3.setDateCreated(match.getTime());
		matchedIdent3.setIdentifier("4321");
		matchedP3.addIdentifier(matchedIdent3);
	   
	    mh = new MatchHandler(); 
	    bestIdentifier = mh.getBestIdentifier(p3,matchedP3,PID.getTime());
	    assertNotNull(bestIdentifier.getIdentifier());
	    System.out.println("best Identifier:\n " + bestIdentifier.getIdentifier()+ " " + bestIdentifier.getPreferred().toString()+ " " + bestIdentifier.getDateCreated().toString());
		
		assertEquals("4321",bestIdentifier.getIdentifier());
	
		for (PatientIdentifier pi : matchedP3.getIdentifiers()){
			System.out.println(pi.getIdentifier()+ " " + pi.getPreferred().toString()+ " " + pi.getDateCreated().toString());
		}
		
		
		//PID newer and not null,  and match null
		
		Patient p4 = new Patient();
		PatientIdentifier PIDIdent4 = new PatientIdentifier();
		PID.set(2007, Calendar.AUGUST, 10);
		PIDIdent4.setDateCreated(PID.getTime());
		PIDIdent4.setIdentifier("1234");
		p4.addIdentifier(PIDIdent4);   
		 
	
		Patient matchedP4 = new Patient();
		match.set(2007, Calendar.JUNE, 1);
		PatientIdentifier matchedIdent4 = new PatientIdentifier();
	    matchedIdent4.setDateCreated(match.getTime());
		matchedIdent4.setIdentifier(null);
		matchedP4.addIdentifier(matchedIdent4);
	   
	    mh = new MatchHandler(); 
	    bestIdentifier = mh.getBestIdentifier(p4, matchedP4, PID.getTime());
	    System.out.println("best Identifier:\n " + bestIdentifier.getIdentifier()+ " " + bestIdentifier.getPreferred().toString()+ " " + bestIdentifier.getDateCreated().toString());
		
		assertEquals("1234",bestIdentifier.getIdentifier());
	
		for (PatientIdentifier pi : matchedP4.getIdentifiers()){
			System.out.println(pi.getIdentifier()+ " " + pi.getPreferred().toString()+ " " + pi.getDateCreated().toString());
		}
		
		
		
	}
	
	@Test
	public void testgetBestName(){
	    
		Calendar PID = Calendar.getInstance();
		Calendar match = Calendar.getInstance();
	
		//PID newer
		Patient p = new Patient();
		PersonName pName = new PersonName();
		PID.set(2007, Calendar.JULY, 10);
		pName.setDateCreated(PID.getTime()); 
		pName.setGivenName("inf");
		pName.setFamilyName("Jones");
		p.addName(pName);
		
		
		Patient matchedP = new Patient();
		PersonName matchedName = new PersonName();
		
		match.set(2007, Calendar.JUNE, 1);
		matchedName.setDateCreated(match.getTime());
		matchedName.setGivenName("John");
		matchedName.setFamilyName("Smith" );
		matchedP.addName(matchedName);
		 
		MatchHandler mh = new MatchHandler();
		
		PersonName bestName = mh.getBestName(p.getPersonName(), matchedP.getPersonName(), PID.getTime());
		
		for (PersonName pn : matchedP.getNames()){
			System.out.println(pn.getGivenName() + " " 
					+ pn.getFamilyName()+ " " + pn.getPreferred()+  pn.getDateCreated().toString());
		}
		assertEquals("Jones", bestName.getFamilyName());
		assertEquals("John", bestName.getGivenName());
		assertEquals("true", bestName.getPreferred().toString());
		assertEquals(pName.getDateCreated(),bestName.getDateCreated());
		
		//TEST 2

		Patient p2 = new Patient();
		PersonName pName2 = new PersonName();
		PID.set(2007, Calendar.SEPTEMBER, 10);
		pName2.setDateCreated(PID.getTime()); 
		pName2.setGivenName("INFANT");
		pName2.setFamilyName("Smith");
		p2.addName(pName2);
		
		
		Patient matchedP2 = new Patient();
		PersonName matchedName2 = new PersonName();
		
		match.set(2007, Calendar.AUGUST, 1);
		matchedName2.setDateCreated(match.getTime());
		matchedName2.setGivenName("John");
		matchedName2.setFamilyName("Smith" );
		matchedName2.setPreferred(true); //the matched patient should already have a setting of preferred.
		matchedP2.addName(matchedName2);
		 
		mh = new MatchHandler();
		
		PersonName bestName2 = mh.getBestName(p2.getPersonName(), matchedP2.getPersonName(),PID.getTime() );
		
		
		for (PersonName pn : matchedP2.getNames()){
			System.out.println(pn.getGivenName() + " " 
					+ pn.getFamilyName()+ " " + pn.getPreferred()+  pn.getDateCreated().toString());
		}
		assertEquals("smith", bestName2.getFamilyName().toLowerCase());
		assertEquals("john", bestName2.getGivenName().toLowerCase());
		assertEquals("true", bestName2.getPreferred().toString());
		assertEquals(pName2.getDateCreated(),bestName2.getDateCreated());
		
//		TEST 3

		Patient p3 = new Patient();
		PersonName pName3 = new PersonName();
		PID.set(2007, Calendar.SEPTEMBER, 10);
		pName3.setDateCreated(PID.getTime()); 
		pName3.setGivenName("INFANT");
		pName3.setFamilyName("SMITH");
		p3.addName(pName3);
		
		
		Patient matchedP3 = new Patient();
		PersonName matchedName3 = new PersonName();
		
		match.set(2007, Calendar.AUGUST, 1);
		matchedName3.setDateCreated(match.getTime());
		matchedName3.setGivenName("Infant");
		matchedName3.setFamilyName("Jones" );
		matchedName3.setPreferred(true); //the matched patient should already have a setting of preferred.
		matchedP3.addName(matchedName3);
		 
		mh = new MatchHandler();
		
		PersonName bestName3 = mh.getBestName(p3.getPersonName(), matchedP3.getPersonName(), PID.getTime());
		
		
		for (PersonName pn : matchedP3.getNames()){
			System.out.println(pn.getGivenName() + " " 
					+ pn.getFamilyName()+ " " + pn.getPreferred()+  pn.getDateCreated().toString());
		}
		assertEquals("SMITH", bestName3.getFamilyName());
		assertEquals("Infant", bestName3.getGivenName());
		assertEquals("true", bestName3.getPreferred().toString());
		assertEquals(pName3.getDateCreated(),bestName3.getDateCreated());
		
	
		
		
	}
	
	@Test
public void testgetBestAddress(){
	    
		Calendar PID = Calendar.getInstance();
		Calendar match = Calendar.getInstance();
	
		//PID newer
		Patient p = new Patient();
		PersonAddress pAddr = new PersonAddress();
		PID.set(2007, Calendar.JULY, 10);
		pAddr.setDateCreated(PID.getTime()); 
		pAddr.setAddress1("55 Green St");
		pAddr.setAddress2("");
		pAddr.setCityVillage("Florida");
		pAddr.setCountry("US");
		pAddr.setCountyDistrict("Marion");
		pAddr.setPostalCode("44444");
		p.addAddress(pAddr);
		
		Patient matchedP = new Patient();
		PersonAddress matchedPAddr = new PersonAddress();
		match.set(2007, Calendar.JUNE, 10);
		matchedPAddr.setDateCreated(match.getTime()); 
		matchedPAddr.setAddress1("22 Red St");
		matchedPAddr.setAddress2("");
		matchedPAddr.setCityVillage("Indy");
		matchedPAddr.setCountry("US");
		matchedPAddr.setCountyDistrict("Marion");
		matchedPAddr.setPostalCode("55555");
		matchedP.addAddress(matchedPAddr);
	
		
		MatchHandler mh = new MatchHandler();
		
		PersonAddress bestAddr = mh.getBestAddress(p, matchedP, PID.getTime());
		
		for (PersonAddress pn : matchedP.getAddresses()){
			System.out.println(pn.getAddress1() + " " 
					+ pn.getAddress2()+ " " 
					+ pn.getCityVillage() + ""
					+ pn.getCountry() + ""
					+ pn.getCountyDistrict() + ""
					+ pn.getPostalCode() + ""	
					+ pn.getPreferred()+  pn.getDateCreated().toString());
		}
		
		
		assertEquals("55 Green St", bestAddr.getAddress1());
		assertEquals("", bestAddr.getAddress2());
		assertEquals("true", bestAddr.getPreferred().toString());
		assertEquals("Florida", bestAddr.getCityVillage());
		assertEquals("US", bestAddr.getCountry());
		assertEquals("Marion", bestAddr.getCountyDistrict());
		assertEquals("44444", bestAddr.getPostalCode());
		assertEquals(pAddr.getDateCreated(),bestAddr.getDateCreated());
		
//		PID older
		Patient p2 = new Patient();
		PersonAddress pAddr2 = new PersonAddress();
		PID.set(2007, Calendar.JULY, 10);
		pAddr2.setDateCreated(PID.getTime()); 
		pAddr2.setAddress1("55 Green St");
		pAddr2.setAddress2("");
		pAddr2.setCityVillage("Florida");
		pAddr2.setCountry("US");
		pAddr2.setCountyDistrict("Marion");
		pAddr2.setPostalCode("44444");
		p2.addAddress(pAddr2);
		
		Patient matchedP2 = new Patient();
		PersonAddress matchedPAddr2 = new PersonAddress();
		match.set(2007, Calendar.AUGUST, 10);
		matchedPAddr2.setDateCreated(match.getTime()); 
		matchedPAddr2.setAddress1("22 August St");
		matchedPAddr2.setAddress2("");
		matchedPAddr2.setCityVillage("Detroit");
		matchedPAddr2.setCountry("US");
		matchedPAddr2.setCountyDistrict("");
		matchedPAddr2.setPostalCode("11111");
		matchedP2.addAddress(matchedPAddr2);
	
		
		MatchHandler mh2 = new MatchHandler();
		
		PersonAddress bestAddr2 = mh2.getBestAddress(p2, matchedP2, PID.getTime());
		
		for (PersonAddress pn : matchedP2.getAddresses()){
			System.out.println(pn.getAddress1() + " " 
					+ pn.getAddress2()+ " " 
					+ pn.getCityVillage() + ""
					+ pn.getCountry() + ""
					+ pn.getCountyDistrict() + ""
					+ pn.getPostalCode() + ""	
					+ pn.getPreferred()+  pn.getDateCreated().toString());
		}
		
		
		assertEquals("22 August St", bestAddr2.getAddress1());
		assertEquals("", bestAddr2.getAddress2());
		assertEquals("true", bestAddr2.getPreferred().toString());
		assertEquals("Detroit", bestAddr2.getCityVillage());
		assertEquals("US", bestAddr2.getCountry());
		assertEquals("", bestAddr2.getCountyDistrict());
		assertEquals("11111", bestAddr2.getPostalCode());
		assertEquals(matchedPAddr2.getDateCreated(),bestAddr2.getDateCreated());
		
		
		
		
		
	}

	@Test
	public void testgetBestProvider(){
	    
		Calendar PID = Calendar.getInstance();
		Calendar match = Calendar.getInstance();
		PersonAttribute pa = new PersonAttribute();
		PersonAttribute paID = new PersonAttribute();
		PersonAttributeType patype = new PersonAttributeType();
		patype.setName("Provider Name");
		patype.setPersonAttributeTypeId(12);	
		
	
		//PID newer
		Patient p = new Patient();
		PID.set(2007, Calendar.JULY, 10);
		
		pa.setValue("John Smith");
		pa.setDateCreated(PID.getTime());
		pa.setAttributeType(patype);
		pa.setPerson(p);
		p.addAttribute(pa);
		
		PersonAttributeType paProvIDtype = new PersonAttributeType();
		paProvIDtype.setName("Provider ID");
		paProvIDtype.setPersonAttributeTypeId(13);
		paID.setAttributeType(paProvIDtype);
		paID.setValue("1111");
		paID.setDateCreated(PID.getTime());
		paID.setPerson(p);
		p.addAttribute(paID);
		
		
		Patient matchedP = new Patient();
		PersonAttribute matchPa = new PersonAttribute();
		PersonAttributeType matchPatype = new PersonAttributeType();
		matchPatype.setName("Provider Name");
		matchPatype.setPersonAttributeTypeId(17);
		matchPa.setAttributeType(matchPatype);
		matchPa.setValue("Frank Jones");
		match.set(2007, Calendar.JUNE, 10);
		matchPa.setDateCreated(match.getTime());
		matchPa.setPerson(matchedP);
		matchedP.addAttribute(matchPa);
		
		PersonAttribute matchPaID = new PersonAttribute();
		PersonAttributeType matchPaIDType = new PersonAttributeType();
		matchPaIDType.setName("Provider ID");
		matchPaIDType.setPersonAttributeTypeId(15);
		matchPaID.setAttributeType(paProvIDtype);
		matchPaID.setValue("2222");
		matchPaID.setDateChanged(match.getTime());
		matchPaID.setPerson(matchedP);
		matchedP.addAttribute(matchPaID);
	}

	



	/*public void testCheckName(){
		
		String fn = "baby";
		String bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		 fn = "baby a";
		bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		 fn = "infant ";
			bestfn = HL7PatientHandler.checkFirstName(fn,false);
			assertEquals("", bestfn);
		
		 fn = "baby 1";
			bestfn = HL7PatientHandler.checkFirstName(fn,false);
			assertEquals("", bestfn);
		
		 fn = "baby boy a";
		 bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		 fn = "babyboy a";
		 bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		 fn = "babyboy";
		 bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		 fn = "infant";
		 bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		 fn = "infant a";
		 bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		fn = "INFANT 1";
		 bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		fn = "inf";
		 bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		fn = "inf a";
		 bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		fn = "boy";
		 bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		fn = "boy extra text";
		 bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("boy extra text", bestfn);
		
		fn = "boy a ";
		 bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		fn = "girl a";
		 bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		
		fn = "inf";
		 bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		fn = "inf";
		 bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("", bestfn);
		
		 fn = "Barbara";
			bestfn = HL7PatientHandler.checkFirstName(fn,false);
			assertEquals("Barbara", bestfn);
		 fn = "";
			bestfn = HL7PatientHandler.checkFirstName(fn,false);
			assertEquals("", bestfn);
		fn = "infantile";
			bestfn = HL7PatientHandler.checkFirstName(fn,false);
		assertEquals("infantile", bestfn);
		
		//contains "inf"
		fn = "Swinford";// Yes I have seen this name in a list of names
		bestfn = HL7PatientHandler.checkFirstName(fn,false);
	    assertEquals("Swinford", bestfn);
	    
	    fn = "JACK";// Yes I have seen this name in a list of names
		bestfn = HL7PatientHandler.checkFirstName(fn,false);
	    assertEquals("JACK", bestfn);
	    
	    fn = "Jones Baby";// Yes I have seen this name in a list of names
		bestfn = HL7PatientHandler.checkFirstName(fn,false);
	    assertEquals("Jones Baby", bestfn);
	    
	    fn = "INFANT D";// Yes I have seen this name in a list of names
		bestfn = HL7PatientHandler.checkFirstName(fn,false);
	    assertEquals("", bestfn);
		
		
	}*/
	
	@Test
	public void testregex () {
		String test = "mrn:;fn:infant a;ln:smith";
		int index1 = test.indexOf("fn:");
		int index2 = test.indexOf(":", index1);
		int index3 = test.indexOf(";", index2);
		String fn = test.substring(index2+1,index3);
		assertEquals("infant a", fn);
	}
	
	/* (non-Javadoc)
	 * @see org.openmrs.test.BaseContextSensitiveTest#useInMemoryDatabase()
	 */
	@Override
	public Boolean useInMemoryDatabase()
	{
		return false;
	}
}
