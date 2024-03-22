package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.ClassOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import java.lang.invoke.MethodHandles;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ConstantHook {
  // first three arguments are here because this is invoked using ConstantDynamic
  public static TrackerPoint constantPoint(MethodHandles.Lookup lookup, String name,
      Class<?> type, int classId, int offset) {
    return TrackerPoint.of(ClassOriginTracker.get(classId), offset);
  }

  // variant for class file versions that don't support ConstantDynamic
  public static TrackerPoint constantPoint(int classId, int offset) {
    return TrackerPoint.of(ClassOriginTracker.get(classId), offset);
  }
}
