package be.coekaerts.wouter.flowtracker.tracker;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

public class TrackerRepository {
  // TODO concurrent weak identity hash map? Use Guava's MapMaker (without pollution classpath)?
  private static final Map<Object, Tracker> objectToTracker =
      Collections.synchronizedMap(new IdentityHashMap<Object, Tracker>());

  public static Tracker getTracker(Object obj) {
    if (!Trackers.isActive()) return null;
    return objectToTracker.get(obj);
  }

  public static Tracker createFixedOriginTracker(Object obj, int length) {
    Tracker tracker = new FixedOriginTracker(length);
    setTracker(obj, tracker);
    return tracker;
  }

  public static Tracker createTracker(Object obj) {
    Tracker tracker = new DefaultTracker();
    setTracker(obj, tracker);
    return tracker;
  }

  public static Tracker getOrCreateTracker(Object obj) {
    Tracker tracker = getTracker(obj);
    return tracker == null ? createTracker(obj) : tracker;
  }

  public static void setInterestTracker(Object obj, Tracker tracker) {
    setTracker(obj, tracker);
    InterestRepository.interestTrackerCreated(tracker);
  }

  public static void setTracker(Object obj, Tracker tracker) {
    if (obj == null) {
      throw new NullPointerException("Can't track null");
    } else if (getTracker(obj) != null) {
      throw new IllegalStateException("Object already has a tracker: " + obj
          + " has " + getTracker(obj));
    } else {
      objectToTracker.put(obj, tracker);
    }
  }
}
