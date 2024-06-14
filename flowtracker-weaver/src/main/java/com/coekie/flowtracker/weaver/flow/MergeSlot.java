package com.coekie.flowtracker.weaver.flow;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A reference to a slot containing a FlowValue in a FlowFrame (in other words, a value on the stack
 * or in a local variable, at a specific instruction), where a merge is happening. Note that as
 * analysis proceeds, the FlowValue that this points to can still change.
 */
class MergeSlot {
  private final FlowFrame frame;
  private final boolean isLocal; // true -> local variable; false -> value on the stack
  private final int index;

  private MergeSlot(FlowFrame frame, boolean isLocal, int index) {
    this.frame = frame;
    this.isLocal = isLocal;
    this.index = index;
  }

  static MergeSlot local(FlowFrame frame, int index) {
    return new MergeSlot(frame, true, index);
  }

  static MergeSlot stack(FlowFrame frame, int index) {
    return new MergeSlot(frame, false, index);
  }

  /**
   * The values that are converging at this MergeSlot.
   * Note that the caller may mutate the returned List.
   */
  List<FlowValue> getMergedValues() {
    int slotIndex = isLocal ? this.index : frame.getLocals() + this.index;
    List<FlowValue> result = frame.mergedValues[slotIndex];
    if (result == null) {
      result = new ArrayList<>();
      frame.mergedValues[slotIndex] = result;
    }
    return result;
  }

  /** Find where `value` is in the frame, and return a {@link MergeSlot} pointing to it */
  // NICE we could avoid looping over all locals and stack, if in (Flow)Frame.merge we would keep
  // of where we are in the loop with merging
  static MergeSlot findMergeSlot(FlowFrame frame, FlowValue value) {
    MergeSlot result = null;
    for (int i = 0; i < frame.getStackSize(); i++) {
      if (frame.getStack(i) == value) {
        if (result != null) {
          throw new IllegalStateException("Found value multiple times");
        }
        result = MergeSlot.stack(frame, i);
      }
    }
    for (int i = 0; i < frame.getLocals(); i++) {
      if (frame.getLocal(i) == value) {
        if (result != null) {
          throw new IllegalStateException("Found value multiple times");
        }
        result = MergeSlot.local(frame, i);
      }
    }
    if (result == null) {
      throw new RuntimeException("Cannot find value " + value);
    }
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MergeSlot)) {
      return false;
    }
    MergeSlot that = (MergeSlot) o;
    return frame == that.frame && isLocal == that.isLocal && index == that.index;
  }

  @Override
  public int hashCode() {
    return Objects.hash(frame, isLocal, index);
  }
}
