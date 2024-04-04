package be.coekaerts.wouter.flowtracker.util;

import be.coekaerts.wouter.flowtracker.tracker.Trackers;

public class Logger {
  private static boolean logging;

  public static void initLogging(Config config) {
    logging = config.getBoolean("logging", false);
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
        t.printStackTrace();
      }
    } finally {
      Trackers.unsuspendOnCurrentThread();
    }
  }


  public void error(String format, Object... args) {
    error(null, format, args);
  }
}
