package org.openmrs.module.sockethl7listener;

import java.io.InputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ca.uhn.hl7v2.app.HL7ServerTestHelper;

/**
 * Adapted version of HAPI HL7ServerTestHelper command line
 * that can be run from a command line
 * @author Meena Sheley and Tammy Dugan
 *
 */
public class CustomHL7ServerTestHelper {
    
    private static final Log ourLog = LogFactory.getLog(CustomHL7ServerTestHelper.class);
    
    /**
     * Main method for running the application
     * 
     * example command lines args:
     * 
     * -f UHN_PRO_DEV_PATIENTS.dat -h 142.224.178.152 -p 3999
     * 
     */
    public static void main( String[] theArgs ) {

        //parse command line arguments        

        //create the command line parser
        CommandLineParser parser = new PosixParser();

        //create the Options
        Options options = new Options();

        options.addOption("h", "host", true, "IP of host to send to");
        options.addOption("p", "port", true, "port to send to");
        options.addOption("f", "file", true, "file to read HL7 messages from");
        
        CommandLine cmdLine = null;
        try
        {
            // parse the command line arguments
            cmdLine = parser.parse(options, theArgs);
        }
        catch (ParseException e)
        {
            ourLog.error(e);
            return;
        }

        String portString = cmdLine.getOptionValue("p");
        int port = 0;
        String host = cmdLine.getOptionValue("h");        
        String file = cmdLine.getOptionValue("f");
        
        if (portString == null || host == null || file == null)
        {
            //automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();
            //assuming that a shell script named serverTest will be created
            formatter.printHelp( "serverTest", options );
            return;
        }
        else {
            //parse portAsString
            port = Integer.parseInt(portString);
        }
        
        HL7ServerTestHelper serverTest = 
        	new  HL7ServerTestHelper( host, port );
        
        //InputStream msgInputStream = HL7ServerTestHelper.class.getResourceAsStream( file );
		InputStream msgInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);		
        try{            
            serverTest.openSocket();
            serverTest.process( msgInputStream );
        }
        catch(Exception e){
        	e.printStackTrace();
            HelpFormatter formatter = new HelpFormatter();
            //assuming that a shell script named hl7mom will be created
            formatter.printHelp( "serverTest", options );
            System.exit(-1);
        }
        
        serverTest.closeSocket();
    }
    
}
