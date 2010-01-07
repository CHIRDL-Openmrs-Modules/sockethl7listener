
package org.openmrs.module.sockethl7listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ca.uhn.hl7v2.app.HL7ServerTestHelper;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileReader;


public class DialogBox 
{

	private static Socket socket = null;
	private static OutputStream os = null;
	private static InputStream is = null;
	
	public static void main(String[] args)
	{
		final JFrame fcframe = new JFrame(); 
		final JTextField t1 = new JTextField(5);
		final JTextField t2= new JTextField(20);
		final JTextField t3= new JTextField(20);
		
		
		class OpenCloseListener implements ActionListener
		{
	        public void actionPerformed(ActionEvent evt) 
	        {
	            JFileChooser chooser = (JFileChooser)evt.getSource();
	            if (JFileChooser.APPROVE_SELECTION.equals(evt.getActionCommand())) {
	                // Open or Save was clicked
	            	File f = chooser.getSelectedFile();
	            	String s = f.getPath();
		            t1.setText(s);
		                
	            	
	            	// Hide dialog
	                fcframe.setVisible(false);
	            } else if (JFileChooser.CANCEL_SELECTION.equals(evt.getActionCommand())) {
	                // Cancel was clicked
	    
	                // Hide dialog
	                fcframe.setVisible(false);
	            }
	            
	        }

		}
		
		class BrowseListener implements ActionListener
		{

			public void actionPerformed(ActionEvent event) 
			{
				JButton b = (JButton)event.getSource();
				JPanel fcpan = new JPanel();
				if (b.getText().equals("Browse"))
				{
				JFileChooser chooser = new JFileChooser("D:\\");
			    chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			         
				fcpan.add(chooser);
				ActionListener cancellist = new OpenCloseListener();
				chooser.addActionListener(cancellist);
				fcframe.add(fcpan);
				fcframe.setSize(600, 370);
				fcframe.setVisible(true);
				
				}
			}
		}
		
		class RunListener implements ActionListener
		{
			public void actionPerformed(ActionEvent event)
			{
				JButton b = (JButton) event.getSource();
				String host = t2.getText();
				
			//	System.out.println("prior to parse int");
				
				int port = Integer.parseInt(t3.getText());
				
				if(b.getText().equals("Run"))
				{		
				
					try{ 
						
						File f = new File(t1.getText());
						
						if (f.isDirectory()) {
							//	prefix = f.getAbsolutePath() + "\\";
					    	File [] myFiles = f.listFiles();	
					    	for(int i=0; i < myFiles.length; i++){
					    		process(myFiles[i], host, port);
					    	}
						}
					    else {
					    	
					    	File file = new File(t1.getText());
					    	process(file, host, port);
					    	
					    }
				   	
					
					} catch (Exception e){
						e.printStackTrace();
					}

				}
			}
			
			public void process(File file, String host, Integer port){
				
				String outputString = null;
				StringBuffer fileData = new StringBuffer(1000);
				
				try {
					openSocket(host,port);
					BufferedInputStream in = 
						new BufferedInputStream(new FileInputStream(file));

					BufferedReader reader = new BufferedReader(
					        new FileReader(file.getAbsolutePath()));
					char[] buf = new char[1024];
					String readData = null;
					int numRead=0;
					while((numRead=reader.read(buf)) != -1){
					    readData = String.valueOf(buf, 0, numRead);
					    fileData.append(readData);
					    buf = new char[1024];
					}
					reader.close();
					
					String[] messages = HL7ServerTestHelper.getHL7Messages(fileData.toString());
			    	for (int i = 0; i < messages.length; i++) {
						sendMessage(messages[i]);
			    	}
					
					
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					closeSocket();
				}
				
				
		        
				
			}
			
			 private void sendMessage(String theMessage) throws IOException
			    {
				 
				 	String Hl7StartMessage = "\u000b";
					String Hl7EndMessage = "\u001c";
			        os.write( Hl7StartMessage.getBytes() );
			        os.write( theMessage.getBytes() );
			        os.write( Hl7EndMessage.getBytes() );
			        os.write(13);
			        os.flush();
			        
			    }
			     
		}
			

			
		
		JFrame frame = new JFrame();
		JLabel filename = new JLabel("Filename: ");
		JLabel hostname = new JLabel("Hostname: ");
		JLabel postnum = new JLabel("Port Number: ");
		JButton browse = new JButton("Browse");
		JButton run = new JButton("Run");
		JPanel pan = new JPanel();
		ActionListener runlist = new RunListener();
		run.addActionListener(runlist);
		ActionListener browselist = new BrowseListener(); 
		browse.addActionListener(browselist);
		pan.setLayout(new BoxLayout(pan, BoxLayout.PAGE_AXIS));
		pan.add(filename);
		pan.add(t1);
		pan.add(browse);
		pan.add(hostname);
		pan.add(t2);
		t2.setText("localhost");
		t3.setText("8765");
		pan.add(postnum);
		pan.add(t3);
		pan.add(run);
		frame.add(pan);
		frame.setSize(600, 200);
		frame.setVisible(true);	
		frame.setTitle("Tester");

		
			
			
			
	}
	
	
	public static void openSocket(String host, Integer port) throws IOException{
        try {
        	socket = new Socket(host, port);
			socket.setSoLinger(true, 10000);
			
			os = socket.getOutputStream();
			is = socket.getInputStream();
			
		} catch (Exception e) {
			
		}
    } 
	
	 public static void closeSocket() {
	        try {
	            Socket sckt = socket;
	            socket = null;
	            if (sckt != null)
	                sckt.close();
	            os.close();
	            is.close();
	        }
	        catch (Exception e) {
	            
	        }
	   }
		

}
