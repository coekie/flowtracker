package be.coekaerts.wouter.flowtracker.weaver.flow;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

/**
 * A value that got copied (e.g. put in a local variable, or loaded onto the stack). We use this to
 * find which instructions a value came from in {@link MergedValue}.
 */
class CopyValue extends FlowValue {
  private final FlowValue original;
  private final AbstractInsnNode insn;

  CopyValue(FlowValue original, AbstractInsnNode insn) {
    super(original.getType());
    this.original = original;
    this.insn = insn;
  }

  @Override
  void ensureTracked() {
    original.ensureTracked();
  }

  @Override
  boolean isTrackable() {
    return original.isTrackable();
  }

  @Override
  AbstractInsnNode getCreationInsn() {
    return insn;
  }

  @Override
  void loadSourceTracker(InsnList toInsert) {
    original.loadSourceTracker(toInsert);
  }

  @Override
  void loadSourceIndex(InsnList toInsert) {
    original.loadSourceIndex(toInsert);
  }

  @Override
  boolean hasMergeAt(FlowFrame mergingFrame) {
    return original.hasMergeAt(mergingFrame);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!super.equals(o)) {
      return false;
    }
    CopyValue other = (CopyValue) o;
    return other.insn == insn && other.original.equals(original);
  }

  static FlowValue copy(FlowValue original, AbstractInsnNode insn) {
    if (original instanceof CopyValue) {
      return copy(((CopyValue) original).original, insn);
    } else {
      return new CopyValue(original, insn);
    }
  }
}
