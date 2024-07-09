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

import java.util.Objects;

/**
 * Pointer to a position in a tracker, representing a single value being tracked, e.g. one byte or
 * one char.
 * <p>
 * A point does have a length, because e.g. a `char` value decoded from a byte[] could come from
 * two bytes.
 */
public class TrackerPoint {
  private static final TrackerDepth depth = TrackerDepth.CONTENT_IMMUTABLE;

  public final Tracker tracker;
  public final int index;
  public final int length;

  private TrackerPoint(Tracker tracker, int index, int length) {
    this.tracker = tracker;
    this.index = index;
    this.length = length;
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
    return index == that.index && Objects.equals(tracker, that.tracker) && length == that.length;
  }

  /** Return a TrackerPoint with same tracker and index as this, but different length */
  public TrackerPoint withLength(int length) {
    // optimization: avoid going through TrackerPoint.of, depth.isAcceptableContent
    return new TrackerPoint(this.tracker, this.index, length);
  }

  @Override
  public int hashCode() {
    return Objects.hash(tracker, index, length);
  }

  /**
   * Create a TrackerPoint with length 1
   * @see #of(Tracker, int, int)
   */
  public static TrackerPoint of(Tracker tracker, int index) {
    return of(tracker, index, 1);
  }

  /**
   * Create a TrackerPoint containing the source of the element at {@code index} in {@code tracker}.
   */
  public static TrackerPoint of(Tracker tracker, int index, int length) {
    if (depth.isAcceptableContent(tracker)) {
      return new TrackerPoint(tracker, index, length);
    } else {
      // possible future optimization: add method in Tracker to read a single value directly.
      // instead of reading it, we let it push it to us
      Gimme gimme = new Gimme();
      tracker.pushSourceTo(index, gimme, 0, length, Growth.NONE);
      return new TrackerPoint(gimme.sourceTracker, gimme.sourceIndex, gimme.sourceLength);
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
    int sourceLength;

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
          this.sourceLength = growth.targetToSource(length);
        } else {
          sourceTracker.pushSourceTo(sourceIndex, this, index, length, growth);
        }
      }
    }
  }
}
