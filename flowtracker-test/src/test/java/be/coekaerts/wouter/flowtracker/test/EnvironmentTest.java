package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.fail;

import org.junit.Test;

public class EnvironmentTest {

  @Test public void noAsm() {
    try {
      EnvironmentTest.class.getClassLoader().loadClass("org.objectweb.asm.ClassReader");
      fail("Asm should not be in the app classpath");
    } catch (ClassNotFoundException expected) {
    }
  }

  @Test public void noWeaver() {
    // this test would fail in IntelliJ, because apparently it doesn't respect the configured
    // "classpathDependencyExclude", even though it should according to
    // https://youtrack.jetbrains.com/issue/IDEA-122783
    StackTraceElement[] stackTrace = new Throwable().getStackTrace();
    if (stackTrace[stackTrace.length - 1].getClassName().contains("intellij")) {
      return;
    }

    try {
      EnvironmentTest.class.getClassLoader()
          .loadClass("be.coekaerts.wouter.flowtracker.weaver.WeaverInitializer");
      fail("Weaver should not be in the app classpath");
    } catch (ClassNotFoundException expected) {
    }
  }
}
