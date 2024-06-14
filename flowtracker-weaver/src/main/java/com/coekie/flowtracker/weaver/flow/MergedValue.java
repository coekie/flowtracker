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

import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;

/**
 * A value that can come from more than one source due to control flow (e.g. due to if-statements or
 * ternary operator).
 */
class MergedValue extends FlowValue {
  private final FlowFrame mergingFrame;
  private final MergeSlot slot;
  private boolean tracked;

  /** Local variable storing the PointTracker for the merged value */
  private TrackLocal pointTrackerLocal;

  private MergedValue(Type type, FlowFrame mergingFrame, MergeSlot slot) {
    super(type);
    this.mergingFrame = mergingFrame;
    this.slot = slot;
  }

  @Override
  boolean isTrackable() {
    // since mergedValues() can still change, we can't _reliably_ return here if we'll be able to
    // do the tracking later (when there may be more values), so just assume that we can.
    return true;
  }

  @Override
  final void ensureTracked() {
    if (!tracked) {
      tracked = true;
      insertTrackStatements();
    }
  }

  @Override
  boolean initCreationFrame(FlowAnalyzer analyzer) {
    if (super.initCreationFrame(analyzer)) {
      for (FlowValue value : mergedValues()) {
        value.initCreationFrame(analyzer);
      }
      return true;
    } else {
      return false;
    }
  }

  private void insertTrackStatements() {
    for (FlowValue value : mergedValues()) {
      if (!value.isTrackable() || !value.hasCreationInsn()) {
        return;
      }
    }

    FlowMethod methodNode = mergingFrame.getMethod();
    pointTrackerLocal = methodNode.newLocalForObject(
        Type.getType("Lcom/coekie/flowtracker/tracker/TrackerPoint;"),
        "MergedValue PointTracker");

    for (FlowValue value : mergedValues()) {
      value.ensureTracked();

      InsnList toInsert = new InsnList();
      methodNode.addComment(toInsert, "MergedValue (TrackerPoint in %s)",
          pointTrackerLocal.getIndex());
      // at the place the value came from, already store its source in our pointTrackerLocal, so
      // that when we get to the point where they get merged, it already has the right TrackerPoint
      value.loadSourcePoint(toInsert, NullFallbackSource.INSTANCE);
      toInsert.add(pointTrackerLocal.store());

      // avoid inserting between a label and a frame; because for verification to succeed the frame
      // must stay right after the label.
      AbstractInsnNode insertAfter = value.getCreationInsn();
      while (insertAfter.getNext() instanceof FrameNode
          || insertAfter.getNext() instanceof LineNumberNode) {
        insertAfter = insertAfter.getNext();
      }

      methodNode.instructions.insert(insertAfter, toInsert);
    }

    InsnList toInsert = new InsnList();
    methodNode.addComment(toInsert, "FYI MergedValue merges here (TrackerPoint in %s)",
        pointTrackerLocal.getIndex());
    methodNode.instructions.insertBefore(mergingFrame.getInsn(), toInsert);
  }

  @Override
  AbstractInsnNode getCreationInsn() {
    return mergingFrame.getInsn();
  }

  @Override
  boolean hasCreationInsn() {
    return true;
  }

  @Override
  void loadSourcePoint(InsnList toInsert, FallbackSource fallback) {
    mergingFrame.getMethod().addComment(toInsert, "MergedValue.loadSourcePoint");
    if (pointTrackerLocal != null) { // if insertTrackStatements didn't bail out
      toInsert.add(pointTrackerLocal.load());
    } else {
      fallback.loadSourcePointFallback(toInsert);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!super.equals(o)) {
      return false;
    }
    MergedValue other = (MergedValue) o;
    return other.mergingFrame == this.mergingFrame && other.slot.equals(this.slot);
  }

  List<FlowValue> mergedValues() {
    return slot.getMergedValues();
  }

  /**
   * Combine two values when possible; creating a MergedValue when necessary. Returns null if the
   * values can't be combined.
   */
  static FlowValue maybeMerge(Type type, FlowFrame mergingFrame, FlowValue value1,
      FlowValue value2) {
    // if we can merge them in-place, do that
    FlowValue merged = value1.mergeInPlace(value2);
    if (merged != null) {
      return merged;
    }

    // this is a "real" merge, so update our mergedValues
    MergeSlot slot = MergeSlot.findMergeSlot(mergingFrame, value1);
    List<FlowValue> mergedValues = slot.getMergedValues();
    if (mergedValues.isEmpty()) { // a new merge
      mergedValues.add(value1);
      mergedValues.add(value2);
    } else { // merge more into an existing merge
      // TODO remove this to handle merges of merges.
      //  skip that case for now because this triggers some bugs (VerifyErrors)
      if ((value1 instanceof MergedValue && !isThisMerge(slot, value1))
        || (value2 instanceof MergedValue && !isThisMerge(slot, value2))) {
        return null;
      }

      addToThisMerge(slot, mergedValues, value1);
      addToThisMerge(slot, mergedValues, value2);
    }

    return new MergedValue(type, mergingFrame, slot);
  }

  static boolean isThisMerge(MergeSlot slot, FlowValue value) {
    return (value instanceof MergedValue) && slot.equals(((MergedValue) value).slot);
  }

  /**
   * Record that `value` gets merged it at `slot`/`mergedValues` (unless value is that merge itself)
   */
  static void addToThisMerge(MergeSlot slot, List<FlowValue> mergedValues, FlowValue value) {
    if (isThisMerge(slot, value)) {
      return;
    }
    // keep mergedValues as small as possible: if the new value can be combined with one of the
    // existing values, do that.
    for (int i = 0; i < mergedValues.size(); i++) {
      FlowValue mergedValue = mergedValues.get(i);
      FlowValue combined = mergedValue.mergeInPlace(value);
      if (combined != null) {
        // if it's not the one that's already in mergedValues, then replace it
        if (combined != mergedValue) {
          mergedValues.set(i, combined);
        }
        return;
      }
    }
    mergedValues.add(value);
  }

  @Override
  FlowValue doMergeInPlace(FlowValue other) {
    if (mergedValues().contains(other)) {
      return this;
    } else if (other instanceof MergedValue
        && ((MergedValue) other).mergedValues().contains(this)) {
      return other;
    } else {
      return null;
    }
  }
}
