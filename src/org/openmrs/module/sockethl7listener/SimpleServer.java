/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.mozilla.org/MPL/
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the License.
 *
 * The Original Code is "SimpleServer.java".  Description:
 * "A simple TCP/IP-based HL7 server."
 *
 * The Initial Developer of the Original Code is University Health Network. Copyright (C)
 * 2002.  All Rights Reserved.
 *
 * Contributor(s): Kyle Buza
 *
 * Alternatively, the contents of this file may be used under the terms of the
 * GNU General Public License (the GPL), in which case the provisions of the GPL are
 * applicable instead of those above.  If you wish to allow use of your version of this
 * file only under the terms of the GPL and not to allow others to use your version
 * of this file under the MPL, indicate your decision by deleting  the provisions above
 * and replace  them with the notice and other provisions required by the GPL License.
 * If you do not delete the provisions above, a recipient may use your version of
 * this file under either the MPL or the GPL.
 */

package org.openmrs.module.sockethl7listener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ContextAuthenticationException;
import org.openmrs.hl7.HL7Service;

import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.llp.LowerLayerProtocol;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.PipeParser;
/**
 * <p>A simple TCP/IP-based HL7 server.  This server listens for connections
 * on a particular port, and creates a ConnectionManager for each incoming
 * connection.  </p>
 * <p>A single SimpleServer can only service requests that use a
 * single class of LowerLayerProtocol (specified at construction time).</p>
 * <p>The ConnectionManager uses a PipeParser of the version specified in
 * the constructor</p>
 * <p>ConnectionManagers currently only support original mode processing.</p>
 * <p>The ConnectionManager routes messages to various "Application"s based on
 * message type.  From the HL7 perspective, an Application is something that
 * does something with a message.</p>
 * @author  Bryan Tripp
 */
public class SimpleServer extends ca.uhn.hl7v2.app.SimpleServer {

   // private static final HapiLog log = HapiLogFactory.getHapiLog(SimpleServer.class);
	private static final Logger log = Logger.getLogger("SimpleServer");
	private static String ADT = "ADT";
    //private int port;
    //private Connection conn;
    private PatientHandler patientHandler = null;
    private HL7SocketHandler socketHandler = null;
    
    private static BufferedWriter bw = null;

    /**
     * Creates a new instance of SimpleServer that listens
     * on the given port.  Exceptions are logged using ca.uhn.hl7v2.Log;
     */
    public SimpleServer(int port, LowerLayerProtocol llp, 
    		Parser parser, 
    		PatientHandler patientHandler,
    		HL7SocketHandler socketHandler){
        super(port, llp, parser);
        //this.port = port;
        this.patientHandler = patientHandler;
        this.socketHandler = socketHandler;

    }
    
//    public SimpleServer(int port, LowerLayerProtocol llp, Parser parser,
//    		String username,String password,
//    		PatientHandler patientHandler,HL7SocketHandler socketHandler) {
//    	this(port,llp,parser,patientHandler,socketHandler);
//    	authenticate(username,password);
//    }
//
//    public Connection getConnection() {
//    	return conn;
//    }
    
//    private void authenticate(String username, String password)
//	{
//    	AdministrationService adminService = Context
//			.getAdministrationService();
//    	
//    	if(username == null)
//    	{
//    		username = adminService
//				.getGlobalProperty("scheduler.username");
//    	}
//    	
//    	if(password == null)
//    	{
//    		password = adminService
//				.getGlobalProperty("scheduler.password");
//    	}
//    	
//		try
//		{	
//			Context.authenticate(username,password );
//
//		} catch (ContextAuthenticationException e)
//		{
//			log.error("Error authenticating user", e);
//		}
//	}
    
    /**
     * Loop that waits for a connection and starts a ConnectionManager
     * when it gets one.
     */
//    @Override
//	public void handle() {
//    	Context.openSession();
//
//		if (Context.isAuthenticated() == false)
//			authenticate(null,null);
//        try {
//            ServerSocket ss = new ServerSocket(port);
//            ss.setSoTimeout(3000);
//            log.info("SimpleServer running on port " + ss.getLocalPort());
//            while (keepRunning()) {
//                try {
//                    Socket newSocket = ss.accept();
//                    log.info("Accepted connection from " + newSocket.getInetAddress().getHostAddress());
//                    conn = new Connection(this.parser, this.llp, newSocket);
//                    newConnection(conn);
//                }
//                catch (InterruptedIOException ie) {
//                    //ignore - just timed out waiting for connection
//                }
//                catch (Exception e) {
//                    log.error( "Error while accepting connections: ", e);
//                }
//            }
//
//            ss.close();
//        }
//        catch (Exception e) {
//            log.error("Openmrs startup exception:",e);
//        }
//        finally {
//            //Bug 960113:  Make sure HL7Service.stop() is called to stop ConnectionCleaner thread.
//        	Context.closeSession();
//            this.stop();
//        }
//    }

    /**
     * Run server from command line.  Port number should be passed as an argument,
     * and a file containing a list of Applications to use can also be specified
     * as an optional argument (as per <code>loadApplicationsFromFile(...)</code>).
     * Uses the default LowerLayerProtocol.
     */
//    public static void main(String args[]) {
//    	
//    	HL7Service hl7Service = Context.getService(org.openmrs.hl7.HL7Service.class);
//        if ( args.length < 3 || args.length > 4) {
//            System.out.println("Usage: ca.uhn.hl7v2.app.SimpleServer port_num username password [application_spec_file_name]");
//            System.exit(1);
//        }
//
//        int port = 0;
//        try {
//            port = Integer.parseInt(args[0]);
//        }
//        catch (NumberFormatException e) {
//            System.err.println("The given port (" + args[0] + ") is not an integer.");
//            System.exit(1);
//        }
//
//        File appFile = null;
//        
//        String username = args[1];
//        String password = args[2];
//        
//        try {
//        	Parser parser = new PipeParser();
//        	PatientHandler patientHandler = new PatientHandler();
//        	HL7SocketHandler socketHandler = new HL7SocketHandler(parser,
//					patientHandler, new HL7ObsHandler25(),
//					new HL7EncounterHandler25(), new HL7PatientHandler25(),null);
//        	socketHandler.setPort(port);
//            SimpleServer server = new SimpleServer(port, LowerLayerProtocol.makeLLP(), 
//            		parser,username,password,patientHandler, socketHandler);
//            if (appFile != null)
//                server.loadApplicationsFromFile(appFile);
//            System.out.println("Starting SimpleServer...");
//            //start time
//            
//            server.start();
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }
    
    public static BufferedWriter getBufferedWriter() throws IOException {
		if (bw == null){
			bw = new BufferedWriter(new FileWriter("hl7Out" + ".txt",true));
		}
		return bw;
	}

	/* (non-Javadoc)
	 * @see ca.uhn.hl7v2.app.HL7Service#newConnection(ca.uhn.hl7v2.app.Connection)
	 */
	@Override
	public synchronized void newConnection(Connection c)
	{
		registerApplication(ADT, "*",socketHandler);
		super.newConnection(c);
	}
		
}
