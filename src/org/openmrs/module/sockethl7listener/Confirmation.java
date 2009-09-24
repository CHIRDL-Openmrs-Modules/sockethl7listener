
package org.openmrs.module.sockethl7listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ca.uhn.hl7v2.app.HL7ServerTestHelper;


public class Confirmation 
{


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
				
			//	System.out.println("port = " + port);
			//	System.out.println("host = " + host);
				
				if(b.getText().equals("Run"))
				{		
					HL7ServerTestHelper serverTest = new HL7ServerTestHelper( host, port );
					try{ 
						serverTest.openSocket();
						File f = new File(t1.getText());
						FileInputStream msgInputStream;
						
					    if (f.isDirectory()) {
							//	prefix = f.getAbsolutePath() + "\\";
					    	File [] myFiles = f.listFiles();	
					    	for(int i=0; i < myFiles.length; i++){
								
								msgInputStream = new FileInputStream(myFiles[i]);
								serverTest.process( msgInputStream );
							}
						}
					    else {
					    	msgInputStream = new FileInputStream(f);
							serverTest.process( msgInputStream );
					    }
				   	
					} catch (FileNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e){
						e.printStackTrace();
					}finally {
						serverTest.closeSocket();
					}

				}
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
		

}
