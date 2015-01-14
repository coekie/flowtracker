package be.coekaerts.wouter.flowtracker.tracker;

import java.util.Map.Entry;

/** Tracker without a source. Its content cannot change, but it can possibly grow. */
public abstract class OriginTracker extends Tracker {

  @Override
  public Entry<Integer, PartTracker> getEntryAt(int index) {
    return null;
  }

  @Override
  public int getEntryCount() {
    return 0;
  }

  @Override
  public boolean isContentMutable() {
    return false;
  }
}
