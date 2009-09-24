
package org.openmrs.module.sockethl7listener;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ca.uhn.hl7v2.app.HL7ServerTestHelper;


public class ATDExporter 
{
	static boolean keepRunning = true;
	static HL7ServerTestHelper serverTest;
	static Logger logger = Logger.getLogger("ATDExporterApp.logger");

	
	
	public static void main(String[] args)
	{
		final JFrame fcframe = new JFrame(); 
		final JTextField t1 = new JTextField(5);
		final JTextField t2= new JTextField(20);
		final JTextField t3= new JTextField(20);
		final JLabel progressText = new JLabel("Processing file:");
		final JLabel progressFile = new JLabel("                                                         ");
		fcframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		try {
			FileHandler handler = new FileHandler ("logs//ATDExporterApp.log");
		    
			logger.addHandler(handler);
			handler.setFormatter(new SimpleFormatter());
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		

		


		final JFrame frame = new JFrame();

		JLabel filename = new JLabel("ATD message directory: ");
		JLabel hostname = new JLabel("Hostname: ");
		JLabel postnum = new JLabel("Port Number: ");

		JButton browse = new JButton("Browse");
		JButton run = new JButton("Run");
		JButton stop = new JButton("Stop");

		final JPanel pan = new JPanel();
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
		//pan.add(stop);
		pan.add(progressText);
		pan.add(progressFile);


		frame.add(pan);
		//frame.setSize(600, 300);

		frame.setMinimumSize(new Dimension(600,225));

		frame.setVisible(true);	
		frame.setTitle("ATDExporter");
		frame.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent we){
				System.exit(0);
			}
		});
		
		
		String host = t2.getText();
		int port = Integer.parseInt(t3.getText());

		serverTest = new HL7ServerTestHelper( host, port );






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
		
		
		
		
			
		
		

		class RunAction implements Runnable {


			public RunAction () { 
			}
			

			public void run () {
				// Your code to do the work, including the loop and GUI updates is all here
				keepRunning = true;
				int numberMessages = 0;
				String host = t2.getText();
				int port = Integer.parseInt(t3.getText());

				HL7ServerTestHelper serverTest = new HL7ServerTestHelper( host, port );
				try{ 
					serverTest.openSocket();
					File f = new File(t1.getText());
					
					FileInputStream msgInputStream;
					File destDir = new File(f.getPath().concat("\\PROCESSED"));
					if (!destDir.exists()) destDir.mkdir();
					
					
					if (f.isDirectory()){
						
						//Continuously check for more files and process
						//Therefore, getting the list of files needs to be performed
						//within the while loop
						
						//while (serverTest(){
						while (keepRunning){
							
							
							File [] myFiles = f.listFiles();
							Pattern p = Pattern.compile("//");
							for (File file : myFiles){
								if (file.isDirectory())break;
								long length = file.length();
								
								int bytesRead = 0;
								int offset = 0;
								String result = "";
								progressFile.setText(file.getAbsolutePath());
								//do what we need to do and place in in bytearrayoutputstream.
								byte[] byteBuffer = new byte[(int)length];
								
								ByteArrayOutputStream out = new ByteArrayOutputStream((int)length);
								msgInputStream = new FileInputStream(file);
								
								while( offset < byteBuffer.length  && 
										(bytesRead = msgInputStream.read(byteBuffer, offset, byteBuffer.length-offset)) >=0){
									out.write(byteBuffer, offset, bytesRead );
									String input =  new String(byteBuffer);
									Matcher m = p.matcher(input);
									result += m.replaceAll("");
									offset += bytesRead;
									
								}
								
								ByteArrayInputStream in = new ByteArrayInputStream(result.getBytes());
								
								progressFile.setText(file.getAbsolutePath());
								numberMessages += serverTest.process( in );
								msgInputStream.close();
							    File outputFile = new File(destDir,file.getName());
							    Calendar now = Calendar.getInstance();
							    
								if (! file.renameTo(new File(destDir,file.getName() + "_"  + now.getTimeInMillis() ))){
									
							    	System.out.println("Unable to move file to output directory.");
							    	logger.log(Level.WARNING, "Unable to move file to output directory");
							    }
								out.flush();
								out.close();
								msgInputStream.close();
								logger.log(Level.INFO, "Processed file: " + file.getName());
								progressFile.setText("");
							}
							
						    Thread.sleep(3000); //poll every 20 secs
						}
					}
					else {
						Pattern p = Pattern.compile("//");
						f.setWritable(true);
					    msgInputStream = new FileInputStream(f);
						progressFile.setText(f.getAbsolutePath());
						//long length = f.length();
						//int bufferSize = 1024;
						//int bytesRead = 0;
						//int l = (int) length;
						//logger.log(Level.ALL, "file length = " + length + " buffer size = " + l);
						//int offset = 0;
						
						//String result = "";
						//do what we need to do and place in in bytearrayoutputstream.
						//byte[] byteBuffer = new byte[(int)length];
						//ByteArrayOutputStream out = new ByteArrayOutputStream((int)length);
						
						//while( offset < byteBuffer.length  && 
						//		(bytesRead = msgInputStream.read(byteBuffer, offset, byteBuffer.length-offset)) >=0){
						//	out.write(byteBuffer, offset, bytesRead );
						//	String input =  new String(byteBuffer);
						//	Matcher m = p.matcher(input);
						//	result += m.replaceAll("");
						//	offset += bytesRead;
							
						//}
						
						//ByteArrayInputStream in = new ByteArrayInputStream(result.getBytes());
						numberMessages += serverTest.process( msgInputStream );
						
						//in.close();
						//out.close();
						msgInputStream.close();
						Calendar now = Calendar.getInstance();
					    if (! f.renameTo(new File(destDir,f.getName() + "_"  + now.getTimeInMillis()))){
					    	System.out.println("Unable to move file to output directory.");
					    	logger.warning("Unable to move file to output directory");
					    	
					    }
					    logger.log(Level.INFO, "Processed file: " + f.getName());
					    progressFile.setText("");
					}

				} catch (FileNotFoundException e1) {
					progressFile.setText("");
					e1.printStackTrace();
					
				} catch (IOException e) {
					progressFile.setText("");
					e.printStackTrace();
				} catch (Exception e){
					progressFile.setText("");
					e.printStackTrace();
				}finally {
					progressFile.setText("");
					serverTest.closeSocket();
					
					System.out.println("Total messages processed = " + numberMessages );
				}


			}
		}

		class RunListener implements ActionListener
		{
			public void actionPerformed(ActionEvent event)
			{
				RunAction ra = new RunAction();
				(new Thread(ra)).start();


			}

		}
		
		class StopAction implements Runnable {


			public StopAction () { 
			}
			

			public void run () {
				serverTest.closeSocket();


			}
		}

		class StopListener implements ActionListener
		{
			public void actionPerformed(ActionEvent event)
			{
				StopAction ra = new StopAction();
				(new Thread(ra)).start();


			}

		}
		
		
		

		ActionListener runlist = new RunListener();
		ActionListener stopList = new StopListener();
		run.addActionListener(runlist);
		stop.addActionListener(stopList);
		ActionListener browselist = new BrowseListener(); 
		browse.addActionListener(browselist);





	}


}
