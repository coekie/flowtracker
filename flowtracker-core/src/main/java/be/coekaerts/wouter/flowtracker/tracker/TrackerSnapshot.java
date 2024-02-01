package be.coekaerts.wouter.flowtracker.tracker;

import static java.util.Objects.requireNonNull;

import be.coekaerts.wouter.flowtracker.hook.StringHook;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable copy of a tracker, that is modeled after {@link WritableTracker}.
 *
 * Just used for testing for now. This is convenient because the WritableTracker interface is the
 * main way we look at what's inside a tracker, so that's what we want our tests to test. But it's
 * not an interface you can directly write assertions on. It's easier to create a snapshot built
 * from it (with {@link #of(Tracker)}) and compare that to an expected snapshot (build with
 * {@link #snapshotBuilder()}).
 */
public class TrackerSnapshot {
  private final List<Part> parts;

  private TrackerSnapshot(List<Part> parts) {
    this.parts = parts;
  }

  /** Create a snapshot of the current content of {@code tracker} */
  public static TrackerSnapshot of(Tracker tracker) {
    Collector collector = new Collector();
    tracker.pushSourceTo(0, tracker.getLength(), collector, 0);
    return new TrackerSnapshot(collector.parts);
  }

  public List<Part> getParts() {
    return parts;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TrackerSnapshot)) return false;
    TrackerSnapshot other = (TrackerSnapshot) o;
    return parts.equals(other.parts);
  }

  @Override public int hashCode() {
    return parts.hashCode();
  }

  @Override public String toString() {
    return parts.toString();
  }

  // this belongs more in test code, but we want to share this between core and other modules,
  // so it's easiest to just have it in here.
  public void assertEquals(TrackerSnapshot other) {
    if (!equals(other)) {
      // this pattern is used because IntelliJ recognizes it
      throw new AssertionError("expected:<" + this + "> but was:<" + other + ">");
    }
  }

  public static Builder snapshotBuilder() {
    return new Builder();
  }

  private static int length(List<Part> parts) {
    if (parts.isEmpty()) return 0;
    Part lastPart = parts.get(parts.size() - 1);
    return lastPart.index + lastPart.length;
  }

  public static final class Part {
    public final int index;
    public final int length;
    public final Tracker source;
    public final int sourceIndex;
    public final Growth growth;

    private Part(int index, int length, Tracker source, int sourceIndex, Growth growth) {
      this.index = index;
      this.length = length;
      this.source = source;
      this.sourceIndex = sourceIndex;
      this.growth = growth;
    }

    @Override public String toString() {
      return "{" + index + "-" + (index + length) + "="
          + (source == null ? "null" : (source + "@" + sourceIndex + growth.toOperationString()))
          + "}";
    }

    @Override public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Part)) return false;
      Part other = (Part) o;
      return index == other.index
          && length == other.length
          && growth.equals(other.growth)
          && source == other.source
          && sourceIndex == other.sourceIndex;
    }

    @Override public int hashCode() {
      return ((index
          * 31 + length)
          * 31 + Objects.hashCode(source.hashCode()))
          * 31 + sourceIndex;
    }
  }

  private static class Collector implements WritableTracker {
    private final List<Part> parts = new ArrayList<>();

    @Override
    public void setSource(int index, int length, Tracker sourceTracker, int sourceIndex, Growth growth) {
      if (index != length(parts)) {
        throw new IllegalStateException("should only append at exactly the end");
      }
      parts.add(new Part(index, length, sourceTracker, sourceIndex, growth));
    }
  }

  /** A builder of a snapshot; to which parts and gaps are added in order from start to end. */
  public static class Builder {
    private final List<Part> parts = new ArrayList<>();

    private Builder() {}

    /** Append a part */
    // TODO make order of arguments consistent with setSource & other part() overloads
    public Builder part(Tracker source, int sourceIndex, int length) {
      return doPart(length, requireNonNull(source), sourceIndex, Growth.NONE);
    }

    /** Append a part */
    public Builder part(int length, Tracker source, int sourceIndex, Growth growth) {
      return doPart(length, requireNonNull(source), sourceIndex, growth);
    }

    /** Append a part containing everything in {@code source} */
    public Builder part(Tracker source) {
      return part(source, 0, source.getLength());
    }

    public Builder track(Object source) {
      if (source instanceof Tracker) throw new IllegalArgumentException("wrong method");
      return part(TrackerRepository.getTracker(source));
    }

    public Builder track(Object source, int sourceIndex, int length) {
      if (source instanceof Tracker) throw new IllegalArgumentException("wrong method");
      return part(TrackerRepository.getTracker(source), sourceIndex, length);
    }

    public Builder trackString(String source) {
      return part(StringHook.getStringTracker(source));
    }

    public Builder trackString(String source, int sourceIndex, int length) {
      return part(StringHook.getStringTracker(source), sourceIndex, length);
    }

    /** Append a part for which the source tracker is unknown */
    public Builder gap(int length) {
      if (length == 0) {
        return this;
      } else {
        return doPart(length, null, -1, Growth.NONE);
      }
    }

    private Builder doPart(int length, Tracker source, int sourceIndex, Growth growth) {
      int index = length(parts); // add new one at the end
      parts.add(new Part(index, length, source, sourceIndex, growth));
      return this;
    }

    public TrackerSnapshot build() {
      return new TrackerSnapshot(parts);
    }

    public void assertEquals(Tracker tracker) {
      build().assertEquals(TrackerSnapshot.of(tracker));
    }

    public void assertTrackerOf(Object obj) {
      assertEquals(TrackerRepository.getTracker(obj));
    }
  }
}
