package be.coekaerts.wouter.flowtracker.weaver.flow;

import java.util.HashSet;
import java.util.Set;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

/**
 * A value that can come from more than one source due to control flow (e.g. due to if-statements or
 * ternary operator).
 */
public class MergedValue extends FlowValue {
  final FlowFrame mergingFrame;
  // NICE: we could optimize this for small sets, like in SourceInterpreter with SmallSet
  private final Set<TrackableValue> values;

  MergedValue(Type type, FlowFrame mergingFrame, FlowValue value1, FlowValue value2) {
    super(type);
    this.mergingFrame = mergingFrame;
    Set<TrackableValue> values = new HashSet<>();
    add(values, value1);
    add(values, value2);
    this.values = values;
  }

  @Override
  boolean isTrackable() {
    return false;
  }

  @Override
  void ensureTracked() {
  }

  @Override
  void loadSourceTracker(InsnList toInsert) {
    toInsert.add(new InsnNode(Opcodes.ACONST_NULL));
  }

  @Override
  void loadSourceIndex(InsnList toInsert) {
    toInsert.add(new InsnNode(Opcodes.ICONST_0));
  }

  private static void add(Set<TrackableValue> values, FlowValue value) {
    if (value instanceof TrackableValue) {
      values.add(((TrackableValue) value));
    } else {
      values.addAll(((MergedValue) value).values);
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
    return other.mergingFrame == this.mergingFrame && other.values.equals(this.values);
  }
}
