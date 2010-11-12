package be.coekaerts.wouter.flowtracker.test;

import junit.framework.Assert;

import org.junit.Test;

public class EnvironmentTest {
	
	@Test
	public void noAsm() {
		try {
			EnvironmentTest.class.getClassLoader().loadClass("org.objectweb.asm.ClassReader");
			Assert.fail("Asm should not be in the app classpath");
		} catch (ClassNotFoundException expected) {
		}
	}
}
