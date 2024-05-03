package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.ClassOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import java.lang.invoke.MethodHandles;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ConstantHook {
  // first three arguments are here because this is invoked using ConstantDynamic
  public static TrackerPoint constantPoint(MethodHandles.Lookup lookup, String name,
      Class<?> type, int classId, int offset, int length) {
    return TrackerPoint.of(ClassOriginTracker.get(classId), offset, length);
  }

  // variant for class file versions that don't support ConstantDynamic
  public static TrackerPoint constantPoint(int classId, int offset) {
    return TrackerPoint.of(ClassOriginTracker.get(classId), offset, 1);
  }

  // we'd rather hava constantPoint(classId, offset, length), but because of limitations of how much
  // stack we can use in ConstantValue.loadSourcePoint we split that into two calls when length
  // is not 1
  public static TrackerPoint withLength(TrackerPoint point, int length) {
    return TrackerPoint.of(point.tracker, point.index, point.length);
  }
}
