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
 * Simplifies tracker info; losing some accuracy/specificity, but making it less noisy, more
 * user-friendly.
 * <p>
 * This merges subsequent parts that are coming from the same source and approximately follow each
 * other in the source together.
 * <p>
 * For example, when mapping a series of bytes to chars, one of the middle chars could be a
 * multi-byte character. In a Tracker that usually means that is represented as three parts: a first
 * part with {@link Growth#NONE}, then one with {@link Growth#HALF}, followed by another with
 * {@link Growth#NONE}. Internally we need that because we want to, as close as we can, track where
 * each index came from exactly, because pieces of it could be cherry-picked. But in the final
 * result, the user only cares about "the thing in this range in the sink comes that that range in
 * the origin", and we don't need to show the exact correspondence of which char maps to which byte.
 * <p>
 * Another example: Repeating the same char many times (e.g. a block of spaces, like indentation,
 * where each space comes from the same place) is seen as one large part instead of a part for each
 * char.
 */
public class Simplifier implements WritableTracker {
  private final WritableTracker delegate;

  // previous part that was pushed into us that hasn't been pushed to the delegate yet.
  private int pendingIndex = -1;
  private Tracker pendingSourceTracker;
  private int pendingSourceIndex;
  private int pendingLength;
  private Growth pendingGrowth;

  Simplifier(WritableTracker delegate) {
    this.delegate = delegate;
  }

  @Override
  public void setSource(int index, int length, Tracker sourceTracker, int sourceIndex,
      Growth growth) {
    if (pendingIndex != -1) {
      if (index == pendingIndex + pendingLength && sourceTracker == pendingSourceTracker) {
        int expectedSourceIndex = pendingSourceIndex + pendingGrowth.targetToSource(pendingLength);
        // lenient check if this follows in the source where we left off, allowing up to
        // growth.sourceBlock difference.
        if (Math.abs(sourceIndex - expectedSourceIndex) <= growth.sourceBlock) {
          pendingLength += length;
          // if it's not an exact match, then represent the whole thing as one block in Growth
          // (because there's probably no way to represent more precisely the correspondence between
          // source and target using just one part).
          if (sourceIndex != expectedSourceIndex || !growth.equals(pendingGrowth)) {
            pendingGrowth = Growth.of(pendingLength,
                sourceIndex + growth.targetToSource(length) - pendingSourceIndex);
          }
          return;
        }
      }

      delegate.setSource(pendingIndex, pendingLength, pendingSourceTracker, pendingSourceIndex,
          pendingGrowth);
    }
    pendingIndex = index;
    pendingLength = length;
    pendingSourceTracker = sourceTracker;
    pendingSourceIndex = sourceIndex;
    pendingGrowth = growth;
  }

  public void flush() {
    if (pendingIndex != -1) {
      delegate.setSource(pendingIndex, pendingLength, pendingSourceTracker, pendingSourceIndex,
          pendingGrowth);
      pendingIndex = -1;
    }
  }

  /**
   * Pushes a simplified version of `source` to `target`.
   */
  public static void simplifySourceTo(Tracker source, WritableTracker target) {
    Simplifier simplifier = new Simplifier(target);
    source.pushSourceTo(0, source.getLength(), simplifier, 0);
    simplifier.flush();
  }
}
