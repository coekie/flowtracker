package be.coekaerts.wouter.flowtracker;

import be.coekaerts.wouter.flowtracker.hook.StringHook;
import be.coekaerts.wouter.flowtracker.hook.SystemHook;
import be.coekaerts.wouter.flowtracker.util.ShutdownSuspender;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class CoreInitializer {
  @SuppressWarnings("UnusedDeclaration") // called with reflection from FlowTrackAgent
  public static void initialize(Map<String, String> config) {
    ensureInitialized();
    SystemHook.initialize();
    StringHook.initDebugUntracked(config.get(StringHook.DEBUG_UNTRACKED));
  }

  // call stuff to make sure JDK internals needed for it are initialized, before we enable tracking
  @SuppressWarnings("WriteOnlyObject")
  private static void ensureInitialized() {
    new ConcurrentSkipListMap<Integer, Integer>().put(1, 1);
  }

  /**
   * Initialize the shutdown hook. This is initialized later, to avoid having a hanging shutdown
   * hook without a UI available to stop it if initialization fails.
   */
  @SuppressWarnings("UnusedDeclaration") // called with reflection from FlowTrackAgent
  public static void postInitialize(Map<String, String> config) {
    ShutdownSuspender.initShutdownHook("true".equals(config.get("suspendShutdown")));
  }
}
