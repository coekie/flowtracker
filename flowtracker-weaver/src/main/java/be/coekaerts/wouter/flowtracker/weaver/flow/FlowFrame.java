package be.coekaerts.wouter.flowtracker.weaver.flow;

import static java.util.Objects.requireNonNull;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

/** {@link Frame} used in flow analysis */
class FlowFrame extends Frame<FlowValue> {
  /** Analyzer that created this frame */
  private final FlowAnalyzer analyzer;

  /** Instruction that this frame corresponds to */
  private AbstractInsnNode insn;

  FlowFrame(int numLocals, int maxStack, FlowAnalyzer analyzer) {
    super(numLocals, maxStack);
    this.analyzer = analyzer;
  }

  FlowFrame(Frame<? extends FlowValue> frame, FlowAnalyzer analyzer) {
    super(frame);
    this.analyzer = analyzer;
  }

  AbstractInsnNode getInsn() {
    if (insn == null) {
      int index = findInsnIndex();
      if (analyzer.methodAdapter.instructions.size() != analyzer.getFrames().length) {
        throw new IllegalStateException("Cannot find insn after code has been changed");
      }
      insn = requireNonNull(analyzer.methodAdapter.instructions.get(index));
    }
    return insn;
  }

  FlowMethodAdapter getFlowMethodAdapter() {
    return analyzer.methodAdapter;
  }

  /** Find the index of the instruction that this frame is for */
  private int findInsnIndex() {
    Frame<FlowValue>[] frames = analyzer.getFrames();
    for (int i = 0; i < frames.length; i++) {
      if (frames[i] == this) {
        return i;
      }
    }
    throw new IllegalStateException("Cannot find this frame in analyzer");
  }

  @Override
  public boolean merge(Frame<? extends FlowValue> frame, Interpreter<FlowValue> interpreter)
      throws AnalyzerException {
    ((FlowInterpreter) interpreter).startMerge(this);
    try {
      return super.merge(frame, interpreter);
    } finally {
      ((FlowInterpreter) interpreter).endMerge();
    }
  }
}
