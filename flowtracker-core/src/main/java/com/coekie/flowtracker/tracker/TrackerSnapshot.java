package com.coekie.flowtracker.tracker;

/*-
 * Copyright 2024 Wouter Coekaerts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.hook.StringHook;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable copy of a tracker, that is modeled after {@link WritableTracker}.
 * <p>
 * Just used for testing for now. This is convenient because the WritableTracker interface is the
 * main way we look at what's inside a tracker, so that's what we want our tests to test. But it's
 * not an interface you can directly write assertions on. It's easier to create a snapshot built
 * from it (with {@link #of(Tracker)}) and compare that to an expected snapshot (build with
 * {@link #snapshot()}).
 */
public class TrackerSnapshot {
  private final List<Part> parts;

  private TrackerSnapshot(List<Part> parts) {
    this.parts = parts;
  }

  /** Create a snapshot of the current content of {@code tracker} */
  public static TrackerSnapshot of(Tracker tracker) {
    return of(tracker, 0, tracker.getLength());
  }

  /** Create a snapshot of a part of the current content of {@code tracker} */
  public static TrackerSnapshot of(Tracker tracker, int index, int length) {
    Collector collector = new Collector();
    tracker.pushSourceTo(index, length, collector, 0);
    return new TrackerSnapshot(collector.parts);
  }

  public List<Part> getParts() {
    return parts;
  }

  public void pushSourceTo(WritableTracker target) {
    for (Part part : parts) {
      target.setSource(part.index, part.length, part.source, part.sourceIndex, part.growth);
    }
  }

  /**
   * Returns a simplified version of this snapshot. This is closer to the state as it would be
   * presented in the UI.
   */
  public TrackerSnapshot simplify() {
    Collector collector = new Collector();
    Simplifier simplifier = new Simplifier(collector);
    pushSourceTo(simplifier);
    simplifier.flush();
    return new TrackerSnapshot(collector.parts);
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

  public static Builder snapshot() {
    return new Builder();
  }

  public static TrackerSubject assertThatTracker(Tracker tracker) {
    return new TrackerSubject(TrackerSnapshot.of(tracker));
  }

  public static TrackerSubject assertThatTrackerOf(Object obj) {
    return new TrackerSubject(TrackerSnapshot.of(TrackerRepository.getTracker(obj)));
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

    public static Part ofPoint(TrackerPoint point) {
      return new Part(0, point.length, point.tracker, point.index, Growth.NONE);
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

    /** Append a part with {@link Growth#NONE} */
    public Builder part(int length, Tracker source, int sourceIndex) {
      return doPart(length, requireNonNull(source), sourceIndex, Growth.NONE);
    }

    /** Append a part */
    public Builder part(int length, Tracker source, int sourceIndex, Growth growth) {
      return doPart(length, requireNonNull(source), sourceIndex, growth);
    }

    /** Append a part pointing to `point` */
    public Builder part(int length, TrackerPoint point) {
      return doPart(length, point.tracker, point.index, Growth.of(length, point.length));
    }

    /**
     * Append a part pointing to `point` with length 1 (which is usually the length when referring
     * to a point, when there is not {@link Growth} involved).
     */
    public Builder part(TrackerPoint point) {
      return part(1, point);
    }

    /** Append a part containing everything in {@code source} */
    public Builder part(Tracker source) {
      return part(source.getLength(), source, 0);
    }

    public Builder track(Object source) {
      if (source instanceof Tracker) throw new IllegalArgumentException("wrong method");
      return part(TrackerRepository.getTracker(source));
    }

    public Builder track(int length, Object source, int sourceIndex) {
      if (source instanceof Tracker) throw new IllegalArgumentException("wrong method");
      return part(length, TrackerRepository.getTracker(source), sourceIndex);
    }

    public Builder trackString(String source) {
      return part(StringHook.getStringTracker(source));
    }

    public Builder trackString(int length, String source, int sourceIndex) {
      return part(length, StringHook.getStringTracker(source), sourceIndex);
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
  }

  // this belongs more in test code, but we want to share this between core and other modules,
  // so it's easiest to just have it in here.
  // this API follows roughly the google-truth example, but this isn't an actual truth "Subject"
  // because this can't depend on google-truth here.
  public static final class TrackerSubject {
    private final TrackerSnapshot snapshot;

    private TrackerSubject(TrackerSnapshot snapshot) {
      this.snapshot = snapshot;
    }

    /**
     * Assert the simplified version of this tracker. This is usually used when the tracker may have
     * a slightly awkward way of being represented internally (e.g. because of Growth stuff),
     * but in the test using this we don't actually care about those details.
     */
    public TrackerSubject simplified() {
      return new TrackerSubject(snapshot.simplify());
    }

    public void matches(TrackerSnapshot.Builder expected) {
      matches(expected.build());
    }

    public void matches(TrackerSnapshot expected) {
      if (!expected.equals(snapshot)) {
        // this pattern is used because IntelliJ recognizes it
        throw new AssertionError("expected:<" + expected + "> but was:<" + snapshot + ">");
      }
    }
  }
}
