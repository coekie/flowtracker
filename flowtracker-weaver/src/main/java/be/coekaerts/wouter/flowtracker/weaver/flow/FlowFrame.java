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
      throw new IllegalStateException("FlowFrame.insn not initialized");
    }
    return insn;
  }

  void initInsn(int insnIndex) {
    if (analyzer.getFrames()[insnIndex] != this) {
      throw new IllegalStateException("Wrong instruction index");
    }
    insn = requireNonNull(analyzer.methodAdapter.instructions.get(insnIndex));
  }

  FlowMethodAdapter getFlowMethodAdapter() {
    return analyzer.methodAdapter;
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
