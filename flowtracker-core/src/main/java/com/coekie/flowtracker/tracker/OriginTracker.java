package com.coekie.flowtracker.tracker;

/** Tracker without a source. Its content cannot change, but it can possibly grow. */
public abstract class OriginTracker extends Tracker {

  @Override
  public int getEntryCount() {
    return 0;
  }

  @Override
  public boolean isContentMutable() {
    return false;
  }
}
