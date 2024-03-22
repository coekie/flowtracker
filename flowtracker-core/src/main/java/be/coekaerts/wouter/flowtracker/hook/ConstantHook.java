package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.ClassOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ConstantHook {
  public static TrackerPoint constantPoint(int classId, int offset) {
    return TrackerPoint.of(ClassOriginTracker.get(classId), offset);
  }
}
