package com.coekie.flowtracker.weaver.flow;

import com.coekie.flowtracker.weaver.Types;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

/** Value for we can't track where it came from */
class UntrackableValue extends FlowValue {
  /**
   * An uninitialized value.
   * @see BasicValue#UNINITIALIZED_VALUE
   */
  static final FlowValue UNINITIALIZED_VALUE = new UntrackableValue(null);

  /** A byte, boolean, char, short, or int value. */
  static final FlowValue INT_VALUE = new UntrackableValue(Type.INT_TYPE);

  /** A float value. */
  static final FlowValue FLOAT_VALUE = new UntrackableValue(Type.FLOAT_TYPE);

  /** A long value. */
  static final FlowValue LONG_VALUE = new UntrackableValue(Type.LONG_TYPE);

  /** A double value. */
  static final FlowValue DOUBLE_VALUE = new UntrackableValue(Type.DOUBLE_TYPE);

  /** An object or array reference value. */
  static final FlowValue REFERENCE_VALUE = new UntrackableValue(Types.OBJECT);

  /** A void value (?!?), used at least for representing return address for JSR instructions */
  static final FlowValue VOID_VALUE = new UntrackableValue(Type.VOID_TYPE);

  UntrackableValue(Type type) {
    super(type);
  }

  @Override
  boolean isTrackable() {
    return false;
  }

  @Override
  void ensureTracked() {
  }

  @Override
  AbstractInsnNode getCreationInsn() {
    return null;
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    toInsert.add(new InsnNode(Opcodes.ACONST_NULL));
  }

  @Override
  boolean hasMergeAt(FlowFrame mergingFrame) {
    return false;
  }
}
