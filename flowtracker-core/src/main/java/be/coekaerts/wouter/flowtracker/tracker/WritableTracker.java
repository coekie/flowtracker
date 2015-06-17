package be.coekaerts.wouter.flowtracker.tracker;

/**
 * Object that can accept writes from
 * {@link Tracker#pushContentToTracker(int, int, WritableTracker, int)}.
 */
public interface WritableTracker {
  /**
   * Set a range of the source of this tracker to the given tracker.
   *
   * If <tt>sourceTracker</tt> is not appropriate as a direct source for this tracker
   * (e.g. because it is not immutable, or is not the original source), this may instead use the
   * source of the source (and recurse) using pushContentToTracker on <tt>sourceTracker</tt>.
   *
   * @param index Index in this tracker
   * @param length Size of the range
   * @param sourceTracker Tracker to use as source
   * @param sourceIndex Index into <tt>sourceTracker</tt>
   */
  void setSourceFromTracker(int index, int length, Tracker sourceTracker, int sourceIndex);
}
