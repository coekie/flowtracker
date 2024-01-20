package be.coekaerts.wouter.flowtracker.tracker;

import java.util.Objects;

/** A position in a tracker, representing a single value being tracked, e.g. one byte or one char */
public class TrackerPoint {
  private static final TrackerDepth depth = TrackerDepth.CONTENT_IMMUTABLE;

  public final Tracker tracker;
  public final int index;
  final Growth growth;

  private TrackerPoint(Tracker tracker, int index, Growth growth) {
    this.tracker = tracker;
    this.index = index;
    this.growth = growth;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TrackerPoint)) {
      return false;
    }
    TrackerPoint that = (TrackerPoint) o;
    return index == that.index && Objects.equals(tracker, that.tracker)
        && Objects.equals(growth, that.growth);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tracker, index, growth);
  }

  /**
   * Create a TrackerPoint with default growth
   * @see #of(Tracker, int, Growth)
   */
  public static TrackerPoint of(Tracker tracker, int index) {
    return of(tracker, index, Growth.NONE);
  }

  public static TrackerPoint ofNullable(Tracker tracker, int index) {
    return tracker == null ? null : of(tracker, index);
  }

  /**
   * Create a TrackerPoint containing the source of the element at {@code index} in {@code tracker}.
   */
  public static TrackerPoint of(Tracker tracker, int index, Growth growth) {
    if (depth.isAcceptableContent(tracker)) {
      return new TrackerPoint(tracker, index, growth);
    } else {
      // perhaps we should add method in reader to read a single value directly?
      // instead of reading it, we let it push it to us
      Gimme gimme = new Gimme();
      tracker.pushSourceTo(index, 1, gimme, 0, growth);
      return new TrackerPoint(gimme.sourceTracker, gimme.sourceIndex, gimme.growth);
    }
  }

  @SuppressWarnings("UnusedDeclaration") // used by instrumented code (ArrayLoadValue)
  public static Tracker getTracker(TrackerPoint trackerPoint) {
    return trackerPoint == null ? null : trackerPoint.tracker;
  }

  @SuppressWarnings("UnusedDeclaration") // used by instrumented code (ArrayLoadValue)
  public static int getIndex(TrackerPoint trackerPoint) {
    return trackerPoint == null ? -1 : trackerPoint.index;
  }

  private static class Gimme implements WritableTracker {
    Tracker sourceTracker;
    int sourceIndex;
    Growth growth;

    @Override
    public void setSource(int index, int length, Tracker sourceTracker, int sourceIndex,
        Growth growth) {
      // usually this is only called once, but if the same value (part of a tracker with length one)
      // is composed of multiple parts (e.g. multi-byte unicode characters) that come from different
      // places (which is weird), then this could be multiple. For simplicity, we only track the
      // first
      if (this.sourceTracker == null && sourceTracker != null) {
        if (depth.isAcceptableContent(sourceTracker)) {
          this.sourceTracker = sourceTracker;
          this.sourceIndex = sourceIndex;
          this.growth = growth;
        } else {
          sourceTracker.pushSourceTo(sourceIndex, length, this, index, growth);
        }
      }
    }
  }
}
