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
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LineNumberNode;

/**
 * A value that can come from more than one source due to control flow (e.g. due to if-statements or
 * ternary operator).
 */
class MergedValue extends FlowValue {
  final FlowFrame mergingFrame;
  // NICE: we could optimize this for small sets, like in SourceInterpreter with SmallSet
  final ValueReference ref;
  private int cachedIsTrackable = -1;
  private boolean tracked;

  /** Local variable storing the PointTracker for the merged value */
  private TrackLocal pointTrackerLocal;

  private MergedValue(Type type, FlowFrame mergingFrame, ValueReference ref) {
    super(type);
    this.mergingFrame = mergingFrame;
    this.ref = ref;
  }

  @Override
  boolean isTrackable() {
    if (cachedIsTrackable == -1) {
      cachedIsTrackable = calcIsTrackable() ? 1 : 0;
    }
    return cachedIsTrackable == 1;
  }

  private boolean calcIsTrackable() {
    for (FlowValue value : mergedValues()) {
      if (!value.isTrackable() || !value.hasCreationInsn()) {
        return false;
      }
    }
    return true;
  }

  @Override
  final void ensureTracked() {
    if (!tracked && isTrackable()) {
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
    if (isTrackable()) {
      toInsert.add(pointTrackerLocal.load());
    } else {
      toInsert.add(new InsnNode(Opcodes.ACONST_NULL));
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
    return other.mergingFrame == this.mergingFrame && other.ref.equals(this.ref);
  }

  Set<FlowValue> mergedValues() {
    return mergingFrame.getMergedValues(ref);
  }

  /**
   * Combine two values when possible; creating a MergedValue when necessary. Returns null if the
   * values can't be combined.
   */
  static FlowValue maybeMerge(Type type, FlowFrame mergingFrame, FlowValue value1,
      FlowValue value2) {
    ValueReference ref = mergingFrame.findReference(value1);

    // if we can merge them in-place, do that
    FlowValue merged = value1.mergeInPlace(value2);
    if (merged != null) {
      return merged;
    }

    // this is a "real" merge, so update our mergedValues
    Set<FlowValue> mergedValues = mergingFrame.getMergedValues(ref);
    if (mergedValues.isEmpty()) { // a new merge
      mergedValues.add(value1);
      mergedValues.add(value2);
    } else { // merge more into an existing merge
      // TODO remove this to handle merges of merges.
      //  skip that case for now because this triggers some bugs (VerifyErrors)
      if ((value1 instanceof MergedValue && !isThisMerge(ref, value1))
        || (value2 instanceof MergedValue && !isThisMerge(ref, value2))) {
        return null;
      }

      addToThisMerge(ref, mergedValues, value1);
      addToThisMerge(ref, mergedValues, value2);
    }

    return new MergedValue(type, mergingFrame, ref);
  }

  static boolean isThisMerge(ValueReference ref, FlowValue value) {
    return (value instanceof MergedValue) && ref.equals(((MergedValue) value).ref);
  }

  /**
   * Record that `value` gets merged it at `ref`/`mergedValues` (unless value is that merge itself)
   */
  static void addToThisMerge(ValueReference ref, Set<FlowValue> mergedValues, FlowValue value) {
    if (isThisMerge(ref, value)) {
      return;
    }
    // keep mergedValues as small as possible: if the new value can be combined with one of the
    // existing values, do that.
    for (FlowValue mergedValue : mergedValues) {
      FlowValue combined = mergedValue.mergeInPlace(value);
      if (combined != null) {
        // if it's not the one that's already in mergedValues, then replace it
        if (combined != mergedValue) {
          mergedValues.remove(mergedValue);
          mergedValues.add(combined);
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
