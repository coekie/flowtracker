package be.coekaerts.wouter.flowtracker;

import be.coekaerts.wouter.flowtracker.hook.StringHook;
import be.coekaerts.wouter.flowtracker.hook.SystemHook;
import be.coekaerts.wouter.flowtracker.util.ShutdownSuspender;
import java.util.Map;

@SuppressWarnings("UnusedDeclaration") // called with reflection from FlowTrackAgent
public class CoreInitializer {
  public static void initialize(Map<String, String> config) {
    SystemHook.initialize();
    StringHook.initDebugUntracked(config.get(StringHook.DEBUG_UNTRACKED));
  }

  /**
   * Initialize the shutdown hook. This is initialized later, to avoid having a hanging shutdown
   * hook without a UI available to stop it if initialization fails.
   */
  public static void postInitialize(Map<String, String> config) {
    ShutdownSuspender.initShutdownHook("true".equals(config.get("suspendShutdown")));
  }
}
