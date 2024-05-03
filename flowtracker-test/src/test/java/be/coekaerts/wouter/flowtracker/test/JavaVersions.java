package be.coekaerts.wouter.flowtracker.test;

import org.junit.AssumptionViolatedException;

public class JavaVersions {
  /** Skip the test if we're running in jdk `version` or newer */
  public static void skipInVersionSince(int version) {
    if (Runtime.version().feature() >= version) {
      throw new AssumptionViolatedException("version >= " + version);
    }
  }
}
