package org.openmrs.module.sockethl7listener.service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openmrs.annotation.Authorized;
import org.openmrs.test.jupiter.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;

/**
 * JUnit tests for the SocketHL7ListenerService.
 * @author Steve McKee
 *
 */
public class SocketHL7ListenerServiceTest extends BaseModuleContextSensitiveTest {
	
	@Test
	@SkipBaseSetup
	void checkAuthorizationAnnotations() throws Exception {
		Method[] allMethods = SocketHL7ListenerService.class.getDeclaredMethods();
		for (Method method : allMethods) {
		    if (Modifier.isPublic(method.getModifiers())) {
		        Authorized authorized = method.getAnnotation(Authorized.class);
		        Assertions.assertNotNull(authorized);
		    }
		}
	}
}
