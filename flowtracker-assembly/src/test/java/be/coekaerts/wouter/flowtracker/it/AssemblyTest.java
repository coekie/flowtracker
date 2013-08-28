// Copyright 2013, Square, Inc.

package be.coekaerts.wouter.flowtracker.it;

import org.junit.Test;

import static org.junit.Assert.fail;

// TODO shouldn't this be part of flowtracker-test?
public class AssemblyTest {

  @Test
  public void testSetupSanity() {
    // The point of this test class is to ensure the assembly works.
    // To properly test that, we must not have anything else than the assembly (and junit) on the
    // classpath.
    for (String classpathPart : System.getProperty("java.class.path").split(":")) {
      if (!classpathPart.contains("assembly") && !classpathPart.contains("junit")) {
        fail("Unexpected classpath in test: " + classpathPart);
      }
    }
  }

  // TODO test if it actually works.
}
