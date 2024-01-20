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

  FlowValue getOriginal() {
    return original;
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
  void initCreationFrame(FlowAnalyzer analyzer) {
    super.initCreationFrame(analyzer);
    original.initCreationFrame(analyzer);
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    original.loadSourcePoint(toInsert);
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
}
