package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ConstantHook {
  public static Tracker constantTracker(String descriptor) {
    Tracker result = new FixedOriginTracker(1);
    result.initDescriptor(descriptor, null);
    return result;
  }
}
