package be.coekaerts.wouter.flowtracker.tracker;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps track of the trackers we're really interested in */
public class InterestRepository {
  private static final ConcurrentHashMap<Long, Tracker> trackers =
      new ConcurrentHashMap<>();

  /**
   * Called when a interesting Tracker has been created.
   *
   * This kind of acts as a listener on TrackerRepository.
   *
   * @param tracker The new tracker
   */
  static void interestTrackerCreated(Tracker tracker) {
    trackers.put(tracker.getTrackerId(), tracker);
  }

  public static Collection<Tracker> getTrackers() {
    return Collections.unmodifiableCollection(trackers.values());
  }

  public static Tracker getContentTracker(long contentTrackerId) {
    return trackers.get(contentTrackerId);
  }
}
