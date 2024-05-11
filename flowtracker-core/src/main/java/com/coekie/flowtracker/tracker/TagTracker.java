package com.coekie.flowtracker.tracker;

/**
 * Tracker that associates an object to a Node in the tree. No tracking of content is done.
 * <p>
 * Arguably this class shouldn't actually extend Tracker; but then they'd have to be stored
 * somewhere else than the {@link TrackerRepository}.
 */
public class TagTracker extends Tracker {
  public TagTracker() {
  }

  @Override public int getEntryCount() {
    throw new UnsupportedOperationException();
  }

  @Override public int getLength() {
    throw new UnsupportedOperationException();
  }
}
