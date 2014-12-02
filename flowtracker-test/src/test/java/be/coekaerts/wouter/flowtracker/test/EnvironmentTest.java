package be.coekaerts.wouter.flowtracker.test;

import org.junit.Test;

import static org.junit.Assert.fail;

public class EnvironmentTest {
	
	@Test
	public void noAsm() {
		try {
			EnvironmentTest.class.getClassLoader().loadClass("org.objectweb.asm.ClassReader");
			fail("Asm should not be in the app classpath");
		} catch (ClassNotFoundException expected) {
		}
	}
}
