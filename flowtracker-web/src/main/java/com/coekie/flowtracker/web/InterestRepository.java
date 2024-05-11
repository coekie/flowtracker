package com.coekie.flowtracker.web;

import com.coekie.flowtracker.tracker.Tracker;
import java.util.concurrent.ConcurrentHashMap;

/** Keeps track of the trackers that have been returned to the UI */
class InterestRepository {
  private static final ConcurrentHashMap<Long, Tracker> trackers =
      new ConcurrentHashMap<>();

  /**
   * Called when a tracker is returned to the UI, so we can later find it back by id.
   * <p>
   * Note that we don't do this for all trackers as soon as they're created as an optimization, and
   * to limit memory usage.
   */
  static void register(Tracker tracker) {
    trackers.put(tracker.getTrackerId(), tracker);
  }

  static Tracker getContentTracker(long contentTrackerId) {
    return trackers.get(contentTrackerId);
  }
}
