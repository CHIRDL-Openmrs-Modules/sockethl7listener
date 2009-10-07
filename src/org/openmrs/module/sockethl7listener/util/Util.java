/**
 * 
 */
package org.openmrs.module.sockethl7listener.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.openmrs.Concept;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.util.OpenmrsUtil;

/**
 * @author tmdugan
 * 
 */
public class Util
{
	public static Properties getProps(String filename)
	{
		try
		{

			Properties prop = new Properties();
			InputStream propInputStream = new FileInputStream(filename);
			prop.loadFromXML(propInputStream);
			return prop;

		} catch (FileNotFoundException e)
		{

		} catch (InvalidPropertiesFormatException e)
		{

		} catch (IOException e)
		{
			// TODO Auto-generated catch block

		}
		return null;
	}
	
	public static Concept lookupConcept(Integer conceptId,String conceptName)
	{
		ConceptService cs = Context.getConceptService();
		
		//lookup by concept id
		Concept concept = cs.getConcept(conceptId);
		
		if(concept == null)
		{
			concept = cs.getConceptByName(conceptName);
		}
		
		return concept;
	}
	
	public static String toProperCase(String str)
	{
		if(str == null || str.length()<1)
		{
			return str;
		}
		
		StringBuffer resultString = new StringBuffer();
		String delimiter = " ";
		
		StringTokenizer tokenizer = new StringTokenizer(str,delimiter,true);
		
		String currToken = null;
		
		while(tokenizer.hasMoreTokens())
		{
			currToken = tokenizer.nextToken();
			
			if(!currToken.equals(delimiter))
			{
				if(currToken.length()>0)
				{
					currToken = currToken.substring(0, 1).toUpperCase()
						+ currToken.substring(1).toLowerCase();
				}
			}
			
			resultString.append(currToken);
		}
		
		return resultString.toString();
	}
	
	public static String computeMD5(String strToMD5) throws DigestException
	{
		try
		{
			//get md5 of input string
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.reset();
			md.update(strToMD5.getBytes());
			byte[] bytes = md.digest();
			
			//convert md5 bytes to a hex string
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < bytes.length; i++)
			{
				hexString.append(Integer.toHexString(0xFF & bytes[i]));
			}
			return hexString.toString();
		} catch (Exception e)
		{
			throw new DigestException("couldn't make digest of partial content");
		}
	}
	
	/**
	 * Adds slashes if needed to a file directory
	 * @param fileDirectory file directory path
	 * @return String formatted file directory path
	 */
	public static String formatDirectoryName(String fileDirectory)
	{
		if(fileDirectory == null||
			fileDirectory.length()==0){
			fileDirectory = OpenmrsUtil.getApplicationDataDirectory();
		}
		
		if(!(fileDirectory.endsWith("/")||fileDirectory.endsWith("\\")))
		{
			fileDirectory+="/";
		}
		return fileDirectory;
	}
	
	public static String convertDateToString(Date date){
		String dateStr = "";
		
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhhmm");
		if (date != null) { 
			dateStr = df.format(date);
		}
		return dateStr;
	}

}
