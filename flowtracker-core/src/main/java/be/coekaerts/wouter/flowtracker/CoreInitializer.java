package be.coekaerts.wouter.flowtracker;

import be.coekaerts.wouter.flowtracker.hook.SystemHook;
import be.coekaerts.wouter.flowtracker.util.ShutdownSuspender;
import java.util.Map;


@SuppressWarnings("UnusedDeclaration") // called with reflection from FlowTrackAgent
public class CoreInitializer {
  public static void initialize(Map<String, String> config) {
    SystemHook.initialize();
    ShutdownSuspender.initShutdownHook("true".equals(config.get("suspendShutdown")));
  }
}
