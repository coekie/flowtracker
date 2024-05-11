package com.coekie.flowtracker.hook;

import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.tracker.TrackerRepository;

@SuppressWarnings("UnusedDeclaration") // used by instrumented code
public class ArrayLoadHook {
  public static TrackerPoint getElementTracker(Object array, int index) {
    Tracker tracker = TrackerRepository.getTracker(array);
    return tracker == null ? null : TrackerPoint.of(tracker, index);
  }
}
