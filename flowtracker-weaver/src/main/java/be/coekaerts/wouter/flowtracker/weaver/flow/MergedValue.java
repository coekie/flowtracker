package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

/**
 * A value that can come from more than one source due to control flow (e.g. due to if-statements or
 * ternary operator).
 */
public class MergedValue extends FlowValue {
  final FlowFrame mergingFrame;
  // NICE: we could optimize this for small sets, like in SourceInterpreter with SmallSet
  private final Set<FlowValue> values;
  private int cachedIsTrackable = -1;
  private boolean tracked;

  private MergedValue(Type type, FlowFrame mergingFrame, Set<FlowValue> values) {
    super(type);
    this.mergingFrame = mergingFrame;
    this.values = values;
  }

  @Override
  boolean isTrackable() {
    if (cachedIsTrackable == -1) {
      cachedIsTrackable = calcIsTrackable() ? 1 : 0;
    }
    // TODO MergedValue.isTrackable. requires fixing FlowFrame.getInsn() getting calculated too late
    //  (when fixing this also remove case for this in InvocationReturnStore)
    return false;
//    return cachedIsTrackable == 1;
  }

  private boolean calcIsTrackable() {
    for (FlowValue value : values) {
      if (!value.isTrackable() || value.getInsn() == null) {
        return false;
      }
    }
    return true;
  }

  @Override
  final void ensureTracked() {
    if (!tracked) {
      tracked = true;
      insertTrackStatements();
    }
  }

  private void insertTrackStatements() {
    for (FlowValue value : values) {
      value.ensureTracked();
    }
    FlowMethodAdapter methodNode = mergingFrame.getFlowMethodAdapter();
    InsnList toInsert = new InsnList();
    methodNode.addComment(toInsert, "MergedValue.insertTrackStatements");
    methodNode.instructions.insertBefore(mergingFrame.getInsn(), toInsert);
  }

  @Override
  AbstractInsnNode getInsn() {
    return mergingFrame.getInsn();
  }

  @Override
  void loadSourceTracker(InsnList toInsert) {
    toInsert.add(new InsnNode(Opcodes.ACONST_NULL));
  }

  @Override
  void loadSourceIndex(InsnList toInsert) {
    toInsert.add(new InsnNode(Opcodes.ICONST_0));
  }

  @Override
  boolean hasMergeAt(FlowFrame mergingFrame) {
    if (mergingFrame == this.mergingFrame) {
      return true;
    }
    for (FlowValue value : values) {
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
    return other.mergingFrame == this.mergingFrame && other.values.equals(this.values);
  }

  /** Add value into values, or return true if a loop is detected */
  private static boolean addAndDetectLoop(Set<FlowValue> values, FlowValue value, FlowFrame mergingFrame) {
    if (value instanceof MergedValue) {
      MergedValue mergedValue = (MergedValue) value;
      if (mergedValue.mergingFrame == mergingFrame) {
        // if the merge is at the same frame, then the merging does not represent two code paths
        // converging, it's us analyzing the same instruction twice, so just combine the results
        values.addAll(mergedValue.values);
        return false;
      } else {
        // avoid problems with loops in the data flow
        if (mergedValue.hasMergeAt(mergingFrame)) {
          return true;
        }
        // TODO we don't have a test case yet where we need a Merge of a Merge;
        //  so for now we just always stop tracking here
        return true;
      }
    }
    values.add(value);
    return false;
  }

  /**
   * Combine two values into a {@link MergedValue} when possible; else return null.
   */
  static MergedValue maybeMerge(Type type, FlowFrame mergingFrame, FlowValue value1,
      FlowValue value2) {
    // if one value is a subset of the other, then this is not two code paths converging; so keep
    // the old MergedValue. Note that this means we keep the old mergingFrame.
    if (value1 instanceof MergedValue) {
      MergedValue mValue1 = (MergedValue) value1;
      if (mValue1.values.contains(value2)) {
        return mValue1;
      }
    }
    if (value2 instanceof MergedValue) {
      MergedValue mValue2 = (MergedValue) value2;
      if (mValue2.values.contains(value1)) {
        return mValue2;
      }
    }

    Set<FlowValue> values = new HashSet<>();
    if (addAndDetectLoop(values, value1, mergingFrame)) {
      return null;
    }
    if (addAndDetectLoop(values, value2, mergingFrame)) {
      return null;
    }

    return new MergedValue(type, mergingFrame, values);
  }
}
