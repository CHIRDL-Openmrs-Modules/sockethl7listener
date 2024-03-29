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

import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.llp.LowerLayerProtocol;
import ca.uhn.hl7v2.parser.Parser;
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

	private static String ADT = "ADT";
    private HL7SocketHandler socketHandler = null;
    

    /**
     * Creates a new instance of SimpleServer that listens
     * on the given port.  Exceptions are logged using ca.uhn.hl7v2.Log;
     */
    public SimpleServer(int port, LowerLayerProtocol llp, 
    		Parser parser, 
    		PatientHandler patientHandler,
    		HL7SocketHandler socketHandler){
        super(port, llp, parser);
        this.socketHandler = socketHandler;
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
