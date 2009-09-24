hl7listenrapp is a standalone app 

1)listens on a port for hl7 messages
2)extracts the information from the MSH,PID,NK1,PV1,OBR,OBX segments.
3)provides demographics to the patient matching module from which that module 
	statistically determines the best match to a patient already existing in the database
4)creates a patient if there was no match found
5)resolves differences between the new demographics and the existing for this patient
6)updates the patient in the database
7)listens for alerts from another module which determines, based on the observations
for that encounter and also for past encounters, if an alert message needs to be sent to one
or more providers for that patient
8)constructs an output hl7 message containing the information that will be sent to the provider


\dist\
	hl7listnerapp.jar
	applicationContext-service.xml
	ehcache.xml
	hibernate.cfg.xml
	HL7listenerapp.jar
	link_config.xml
	log4j.properties
	OPENMRS-runtime.properties
	run.bat
	Tester.jar
\lib\
	antlr_2.7.6.jar
	asm-attrs.jar
	asm.jar
	c3p0-0.9.1.jar
	cglib-2.1_3.jar
	commons-cli-1.0.jar
	commons-collections-3.1.jar
	commons-logging-1.0.4.jar
	commons-logging-api.jar
	commons-logging.jar
	commons-logging.license
	dom4j-1.6.1.jar
	ehcache-1.2.4.jar
	hapi-0.5.1.jar
	hibernate3.jar
	impl
	jakarta-log4j
	jta.jar
	junit-addons-1.4.jar
	log4j
	log4j-1.2.15.jar
	log4j.license
	mail
	mysql-connector-java-3.1.10-bin.jar
	naming-resources.jar
	openmrs-api-1.1.10.2432.jar
	patientmatching-1.0.5.jar
	spring-framework
	vssver.scc
	xmlParserAPIs.jar

s