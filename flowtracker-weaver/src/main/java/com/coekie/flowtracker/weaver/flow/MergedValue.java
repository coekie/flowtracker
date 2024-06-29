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
 * A {@link FlowValue} that can come from more than one source {@link FlowValue} depending on
 * control flow (e.g. due to if-statements, loops, or ternary operator).
 * <p>
 * For example, consider code like this:
 * <pre>{@code
 * byte b;
 * if (condition) {
 *   b = value1;
 * } else {
 *   b = value2;
 * }
 * somethingElse();
 * store(b);
 * }</pre>
 * At `b = value1()` the FlowValue for local variable `b` is CopyValue(value1), at `b = value2()`
 * it's CopyValue(value2), and starting at the instruction/frame where the control flow converges,
 * at `somethingElse()`, it is a `MergedValue` where {@link MergedValue#mergedValues()} contains
 * those two values.
 * <p>
 * About {@link FlowValue#mergeInPlace(FlowValue)}: In the example above, while it is being
 * analyzed, in a first pass that takes the first branch, at `somethingElse()` and `store(b)`, b
 * could have CopyValue(value1). In a second pass that takes the other branch, the MergedValue is
 * created at `somethingElse()`. Then at store(b) we have to merge the CopyValue(value1) that's
 * still there from the first pass, with the MergedValue from the previous frame. We don't create a
 * new MergedValue there, but mergeInPlace returns the existing MergedValue.
 * <p>
 * How do we instrument this; how do we know at the `store(b)` instruction where b came from?
 * We create a local variable ({@link #pointTrackerLocal}), and assign the TrackerPoint to it at the
 * creation of all {@link MergedValue#mergedValues()}, so at `b = value1` and `b = value2`.
 * <p>
 * Side note: For this example, this seems a bit overcomplicated. You could think that it would be
 * simpler to associate the {@link #pointTrackerLocal} to the local variable (for every relevant
 * local variable), instead of with places where values merge, so that we don't need to care about
 * merges at all. But that doesn't handle the case when values on the stack get merged, e.g. with
 * the ternary operator (`b = condition ? value1 : value2`).
 *
 * @see MergeSlot
 */
class MergedValue extends FlowValue {
  /**
   * Frame where the merge happened. In the example above that is the frame for `somethingElse()`.
   */
  private final FlowFrame mergingFrame;

  /**
   * Slot in the {@link #mergingFrame} where the merge happened. We use this to indirectly store the
   * {@link #mergedValues()}. This indirection (as opposed to storing the mergedValues as a field
   * in MergedValue directly) makes us able to handle loops, where a MergedValue (usually
   * indirectly, through {@link CopyValue}s) points back to itself.
   */
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

      // avoid inserting instructions between a label and a frame; because for verification to
      // succeed the frame must stay right after the label if it's a jump target.
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
   * Combine two values into a MergedValue. Returns null if the values can't be merged.
   * This is only called if we actually need a MergedValue; if the values can't be merged
   * {@link FlowValue#mergeInPlace(FlowValue) in-place}.
   */
  static MergedValue merge(Type type, FlowFrame mergingFrame, FlowValue value1, FlowValue value2) {
    MergeSlot slot = MergeSlot.findMergeSlot(mergingFrame, value1);
    List<FlowValue> mergedValues = slot.getMergedValues();
    if (mergedValues.isEmpty()) { // a new merge
      mergedValues.add(value1);
      mergedValues.add(value2);
    } else { // merge more into an existing merge
      // TODO we're skipping handling merges of merges for now, because this triggers some bugs
      //  (VerifyErrors). Note that we _do_ handle e.g. merges of CopyValues of merges, so we do
      //  handle some complex control flows, but not all of them.
      // My suspicion is those bugs are related to where exactly we would insert our code that sets
      // the value of the pointTrackerLocal. Most other instrumentation targets its code injection
      // before or after a particular operation; but merges can happen at various different kinds of
      // places, and for some of them we may have to tweak exactly where we insert our code.
      // We already have some of that tweaking for FrameNode and LineNumberNode, but we might need
      // more like that.
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
