package be.coekaerts.wouter.flowtracker.util;

import be.coekaerts.wouter.flowtracker.tracker.Trackers;

/**
 * Handles logging for flowtracker. Deliberately very primitive, not using a _proper_ logging
 * framework.
 */
public class Logger {
  private static boolean logging;
  private static boolean exitOnError;

  public static void initLogging(Config config) {
    logging = config.getBoolean("logging", false);
    exitOnError = config.getBoolean("exitOnError", false);
  }

  private final String name;

  public Logger(String name) {
    this.name = name;
  }

  public void info(String format, Object... args) {
    if (logging) {
      info(String.format(format, args));
    }
  }

  public void info(String str) {
    if (logging) {
      System.err.println("[FT " + name + "] " + str);
    }
  }

  public void error(Throwable t, String format, Object... args) {
    // suspend tracking to avoid possibility of errors during logging errors, which might recurse
    Trackers.suspendOnCurrentThread();
    try {
      System.err.println("[FT " + name + " ERROR] " + String.format(format, args));
      if (t != null) {
        //noinspection CallToPrintStackTrace
        t.printStackTrace();
      }
      if (exitOnError) {
        // forcefully exit, don't even try to run shutdown hooks. this avoids stuff going crazy if
        // our instrumentation has caused something fundamental to fail.
        Runtime.getRuntime().halt(1);
      }
    } finally {
      Trackers.unsuspendOnCurrentThread();
    }
  }

  public void error(String format, Object... args) {
    error(null, format, args);
  }
}
