package be.coekaerts.wouter.flowtracker;

import be.coekaerts.wouter.flowtracker.hook.ClassLoaderHook;
import be.coekaerts.wouter.flowtracker.hook.StringHook;
import be.coekaerts.wouter.flowtracker.hook.SystemHook;
import be.coekaerts.wouter.flowtracker.hook.ZipFileHook;
import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.DefaultTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.util.Config;
import be.coekaerts.wouter.flowtracker.util.ShutdownSuspender;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

public class CoreInitializer {
  public static void initialize(Config config, JarFile agentJar) {
    ensureInitialized();
    Tracker.initialize(config);
    ZipFileHook.initialize(config, agentJar);
    SystemHook.initialize(config);
    StringHook.initialize(config);
    ClassLoaderHook.initialize(config);
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
