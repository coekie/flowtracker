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
      if (insertAfter.getNext() instanceof FrameNode) {
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
  boolean hasMergeAt(FlowFrame mergingFrame) {
    if (mergingFrame == this.mergingFrame) {
      return true;
    }
    for (FlowValue value : mergedValues()) {
      if (value.hasMergeAt(mergingFrame)) {
        return true;
      }
    }
    return false;
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
   * Combine two values into a {@link MergedValue} when possible; else return null.
   */
  static FlowValue maybeMerge(Type type, FlowFrame mergingFrame, FlowValue value1,
      FlowValue value2) {
    // if one value is a subset of the other, then this is not two code paths converging; so keep
    // the old MergedValue. Note that this means we keep the old mergingFrame.
    if (contains(value1, value2)) {
      return value1;
    }
    if (contains(value2, value1)) {
      return value2;
    }

    ValueReference ref = mergingFrame.findReference(value1);
    Set<FlowValue> mergedValues = mergingFrame.getMergedValues(ref);
    if (!((value1 instanceof MergedValue) && ((MergedValue) value1).ref.equals(ref))) {
      if (value1 instanceof MergedValue) { // TODO remove. for now still excluding merges of merges
        return null;
      }
      mergedValues.add(value1);
    }
    if (!((value2 instanceof MergedValue) && ((MergedValue) value2).ref.equals(ref))) {
      if (value2 instanceof MergedValue) { // TODO remove. for now still excluding merges of merges
        return null;
      }
      mergedValues.add(value2);
    }

    return new MergedValue(type, mergingFrame, ref);
  }

  /**
   * Returns if value1 contains value2, either directly (when value1 is a MergedValue) or indirectly
   * (nested into CopyValue)
   */
  static boolean contains(FlowValue value1, FlowValue value2) {
    if (value1.equals(value2)) {
      return true;
    }

    if (value1 instanceof MergedValue) {
      for (FlowValue value : ((MergedValue) value1).mergedValues()) {
        if (contains(value, value2)) {
          return true;
        }
      }
    } else if (value1 instanceof CopyValue && value2 instanceof CopyValue) {
      CopyValue cValue1 = (CopyValue) value1;
      CopyValue cValue2 = (CopyValue) value2;
      if (cValue1.getCreationInsn() == cValue2.getCreationInsn()) {
        return contains(cValue1.getOriginal(), cValue2.getOriginal());
      }
    }

    return false;
  }
}
