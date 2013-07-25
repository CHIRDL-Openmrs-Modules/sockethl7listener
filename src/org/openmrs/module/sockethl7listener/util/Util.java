/**
 * 
 */
package org.openmrs.module.sockethl7listener.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openmrs.Concept;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;

/**
 * @author tmdugan
 * 
 */
public class Util
{	
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
	
	public static String convertDateToString(Date date){
		String dateStr = "";
		
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmm");
		if (date != null) { 
			dateStr = df.format(date);
		}
		return dateStr;
	}

}
