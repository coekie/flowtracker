package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ArrayLoadHook {
  public static TrackerPoint getElementTracker(Object array, int index) {
    Tracker tracker = TrackerRepository.getTracker(array);
    return tracker == null ? null : TrackerPoint.of(tracker, index);
  }
}
