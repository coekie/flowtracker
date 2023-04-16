package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

/** A value of which we can track where it came from */
abstract class TrackableValue extends FlowValue {
  final FlowMethodAdapter flowMethodAdapter;
  private final AbstractInsnNode insn;
  private boolean tracked;

  TrackableValue(FlowMethodAdapter flowMethodAdapter, Type type, AbstractInsnNode insn) {
    super(type);
    this.flowMethodAdapter = flowMethodAdapter;
    this.insn = insn;
  }

  @Override
  boolean isTrackable() {
    return true;
  }

  @Override
  final void ensureTracked() {
    if (!tracked) {
      insertTrackStatements();
      tracked = true;
    }
  }

  @Override
  AbstractInsnNode getInsn() {
    return insn;
  }

  /**
   * Insert the statements needed to keep track of the origin of this value.
   * <p>
   * This method should not be called directly, instead {@link #ensureTracked()}
   * should be used, to ensure statements are not inserted more than once.
   */
  abstract void insertTrackStatements();

  @Override
  boolean hasMergeAt(FlowFrame mergingFrame) {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    return o == this || (super.equals(o) && ((TrackableValue) o).insn == this.insn);
  }
}
