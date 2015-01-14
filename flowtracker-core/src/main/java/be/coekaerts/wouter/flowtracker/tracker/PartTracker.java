package be.coekaerts.wouter.flowtracker.tracker;

/** Part of a {@link Tracker} that comes from one source. */
public class PartTracker extends Tracker {
  private final Tracker tracker;
  private final int index;
  private int length;

  public PartTracker(Tracker tracker, int index, int length) {
    this.tracker = tracker;
    this.index = index;
    this.length = length;
  }

  /**
   * The tracker that this part points to.
   *
   * This is the source; this is <b>not</b> the tracker of which this is a part.
   */
  public Tracker getTracker() {
    return tracker;
  }

  /**
   * The index in {@link #getTracker()} where this part starts
   */
  public int getIndex() {
    return index;
  }

  /** The length of this part */
  public int getLength() {
    return length;
  }

  void setLength(int length) {
    this.length = length;
  }

  @Override
  public int getEntryCount() {
    return 1;
  }

  @Override
  public void pushContentToTracker(int sourceIndex, int length, Tracker targetTracker,
      int targetIndex) {
    targetTracker.setSourceFromTracker(targetIndex, length, tracker, index + sourceIndex);
  }
}
