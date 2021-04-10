package be.coekaerts.wouter.flowtracker.tracker;

/** Part of a {@link Tracker} that comes from one source. */
public class PartTracker extends Tracker {
  private final Tracker tracker;
  private final int sourceIndex;
  private int length;
  private final Growth growth;

  public PartTracker(Tracker tracker, int sourceIndex, int length, Growth growth) {
    this.tracker = tracker;
    this.sourceIndex = sourceIndex;
    this.length = length;
    this.growth = growth;
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
  public int getSourceIndex() {
    return sourceIndex;
  }

  /** The length of this part */
  public int getLength() {
    return length;
  }

  void setLength(int length) {
    this.length = length;
  }

  Growth getGrowth() {
    return growth;
  }

  @Override
  public int getEntryCount() {
    return 1;
  }

  @Override
  public void pushSourceTo(int sourceIndex, int targetLength, WritableTracker targetTracker,
      int targetIndex, Growth growth) {
    targetTracker.setSource(targetIndex, targetLength, tracker, this.sourceIndex + sourceIndex,
        this.growth.combine(growth));
  }
}
