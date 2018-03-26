package org.openmrs.module.sockethl7listener.service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Test;
import org.openmrs.annotation.Authorized;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.test.SkipBaseSetup;

/**
 * JUnit tests for the SocketHL7ListenerService.
 * @author Steve McKee
 *
 */
public class SocketHL7ListenerServiceTest extends BaseModuleContextSensitiveTest {
	
	@Test
	@SkipBaseSetup
	public void checkAuthorizationAnnotations() throws Exception {
		Method[] allMethods = SocketHL7ListenerService.class.getDeclaredMethods();
		for (Method method : allMethods) {
		    if (Modifier.isPublic(method.getModifiers())) {
		        Authorized authorized = method.getAnnotation(Authorized.class);
		        Assert.assertNotNull("Authorized annotation not found on method " + method.getName(), authorized);
		    }
		}
	}
}
