package com.coekie.flowtracker;

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

import com.coekie.flowtracker.hook.ClassLoaderHook;
import com.coekie.flowtracker.hook.StringHook;
import com.coekie.flowtracker.hook.SystemHook;
import com.coekie.flowtracker.hook.ZipFileHook;
import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.DefaultTracker;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.util.Config;
import com.coekie.flowtracker.util.Logger;
import com.coekie.flowtracker.util.RecursionChecker;
import com.coekie.flowtracker.util.ShutdownSuspender;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

public class CoreInitializer {

  /**
   * Initialization before the weaver is installed
   */
  public static void preInitialize(Config config) {
    Logger.initLogging(config);
    RecursionChecker.initialize(config);
    ClassLoaderHook.initialize(config);
  }

  public static void initialize(Config config, JarFile agentJar) {
    ensureInitialized();
    Tracker.initialize(config);
    ZipFileHook.initialize(config, agentJar);
    SystemHook.initialize(config);
    StringHook.initialize(config);
  }

  // call stuff to make sure JDK internals needed for it are initialized, before we enable tracking
  // e.g. java.util.concurrent.ConcurrentSkipListMap
  private static void ensureInitialized() {
    DefaultTracker tracker1 = new DefaultTracker();
    tracker1.setSource(0, 1, new ByteOriginTracker(), 7);
    tracker1.pushSourceTo(0, 1, new DefaultTracker(), 10);
  }

  /**
   * Initialize the shutdown hook. This is initialized later, to avoid having a hanging shutdown
   * hook without a UI available to stop it if initialization fails.
   */
  @SuppressWarnings("UnusedDeclaration") // called with reflection from FlowTrackAgent
  public static void postInitialize(Config config) {
    ShutdownSuspender.initShutdownHook(config.getBoolean("suspendShutdown", false));
    verifyJvmArgs();
  }

  /**
   * Verify that the JVM options without which flowtracker doesn't work properly have been used.
   * (I added this check because I've wasted too much time debugging issues where something was
   * unexpectedly not tracked because I forgot this).
   */
  private static void verifyJvmArgs() {
    // note: we keep this as a single string on one long line here to make this easier to copy-paste
    // keep this in sync with pom.xml
    String expectedJvmArgsString = "-XX:-UseStringDeduplication -XX:+UnlockDiagnosticVMOptions -XX:DisableIntrinsic=_copyOf -XX:DisableIntrinsic=_copyOfRange -XX:DisableIntrinsic=_String_String -XX:DisableIntrinsic=_StringBuilder_String -XX:DisableIntrinsic=_StringBuilder_append_char -XX:DisableIntrinsic=_StringBuilder_append_String -XX:DisableIntrinsic=_StringBuilder_toString -XX:DisableIntrinsic=_inflateStringC -XX:DisableIntrinsic=_inflateStringB -XX:DisableIntrinsic=_toBytesStringU -XX:DisableIntrinsic=_getCharsStringU -XX:DisableIntrinsic=_getCharStringU -XX:DisableIntrinsic=_putCharStringU -XX:DisableIntrinsic=_compressStringC -XX:DisableIntrinsic=_compressStringB -XX:DisableIntrinsic=_encodeByteISOArray";
    String[] expectedJvmArgs = expectedJvmArgsString.split(" ");
    Set<String> givenJvmArgs =
        new HashSet<>(ManagementFactory.getRuntimeMXBean().getInputArguments());
    for (String expectedJvmArg : expectedJvmArgs) {
      if (!givenJvmArgs.contains(expectedJvmArg)) {
        throw new Error("JVM should be started with: " + expectedJvmArgsString + "\n"
            + "Not found: " + expectedJvmArg);
      }
    }
  }
}
