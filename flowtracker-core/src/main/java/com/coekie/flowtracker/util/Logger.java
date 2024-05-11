package com.coekie.flowtracker.util;

/*-
 * Copyright 2024 Wouter Coekaerts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.coekie.flowtracker.tracker.Trackers;

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
