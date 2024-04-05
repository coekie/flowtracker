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
import java.util.jar.JarFile;

public class CoreInitializer {
  public static void initialize(Config config, JarFile agentJar) {
    ensureInitialized();
    Tracker.initialize(config);
    ZipFileHook.initialize(config, agentJar);
    SystemHook.initialize();
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
  }
}
