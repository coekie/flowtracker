package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ConstantHook {
  public static Tracker constantTracker(String descriptor) {
    Tracker result = new FixedOriginTracker(1);
    result.initDescriptor(descriptor);
    return result;
  }

  public static TrackerPoint constantPoint(String descriptor) {
    return TrackerPoint.of(constantTracker(descriptor), 0);
  }
}
