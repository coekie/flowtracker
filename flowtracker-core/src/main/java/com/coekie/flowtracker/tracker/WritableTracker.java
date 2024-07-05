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

/**
 * Object that can accept writes from
 * {@link Tracker#pushSourceTo(int, WritableTracker, int, int, Growth)}.
 */
public interface WritableTracker {
  /**
   * Set a range of the source of this tracker to the given tracker, with {@link Growth#NONE}.
   *
   * @see #setSource(int, int, Tracker, int, Growth)
   */
  default void setSource(int index, int length, Tracker sourceTracker, int sourceIndex) {
    setSource(index, length, sourceTracker, sourceIndex, Growth.NONE);
  }

  /**
   * Set a range of the source of this tracker to the given `point`.
   *
   * @see #setSource(int, int, Tracker, int, Growth)
   */
  default void setSource(int index, int length, TrackerPoint point) {
    setSource(index, length, point.tracker, point.index, Growth.of(length, point.length));
  }

  /**
   * Set a range of the source of this tracker to the given tracker.
   * <p>
   * If <tt>sourceTracker</tt> is not appropriate as a direct source for this tracker
   * (e.g. because it is not immutable, or is not the original source), this may instead use the
   * source of the source (and recurse) using pushContentToTracker on <tt>sourceTracker</tt>.
   *
   * @param index Index in this tracker
   * @param length Size of the range in this tracker
   * @param sourceTracker Tracker to use as source
   * @param sourceIndex Index into <tt>sourceTracker</tt>
   * @param growth the correspondence between the source and target range. This also determines the
   *   length of the relevant range in the source (sourceLength). `length` should be a multiple of
   *   this {@link Growth#targetBlock}.
   */
  void setSource(int index, int length, Tracker sourceTracker, int sourceIndex,
      Growth growth);
}
