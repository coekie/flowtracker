package be.coekaerts.wouter.flowtracker.tracker;

/**
 * Object that can accept writes from
 * {@link Tracker#pushSourceTo(int, int, WritableTracker, int)}.
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
  // TODO remove this overload
  default void setSource(int index, int length, Tracker sourceTracker, int sourceIndex) {
    setSource(index, length, sourceTracker, sourceIndex, Growth.NONE);
  }

  // TODO remove the default implementation
  // TODO this needs more thought, see NOTES
  // TODO document behaviour (or intention) for copying part of a block (character) from source:
  //   in order to know for each index in the target from which source block they come, either
  //   sourceIndex needs to point to the start of a block, or it needs to be a short length (shorter
  //   than Growth.something)
  void setSource(int index, int length, Tracker sourceTracker, int sourceIndex,
      Growth growth);
}
