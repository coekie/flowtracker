package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

/** Extension of {@link Analyzer}, used for flow analysis */
class FlowAnalyzer extends Analyzer<FlowValue> {
  final FlowMethodAdapter methodAdapter;

  FlowAnalyzer(FlowInterpreter interpreter, FlowMethodAdapter methodAdapter) {
    super(interpreter);
    this.methodAdapter = methodAdapter;
  }

  @Override
  protected Frame<FlowValue> newFrame(int numLocals, int numStack) {
    return new FlowFrame(numLocals, numStack, this);
  }

  @Override
  protected Frame<FlowValue> newFrame(Frame<? extends FlowValue> frame) {
    return new FlowFrame(frame, this);
  }

  @Override
  public Frame<FlowValue>[] analyze(String owner, MethodNode method) throws AnalyzerException {
    Frame<FlowValue>[] frames = super.analyze(owner, method);

    for (int i = 0; i < frames.length; i++) {
      // note: if we need this to be initialized earlier, we could also override
      // newControlFlowEdge & newControlFlowExceptionEdge and initialize it at that point
      FlowFrame frame = (FlowFrame) frames[i];
      if (frame != null) {
        frame.initInsn(i);
      }
    }

    return frames;
  }

  /**
   * Gets the FlowFrame that represents the state of local variables and stack at the given
   * instruction. This must be called after the analyzer ran, but before any changes have been
   * made.
   */
  FlowFrame getFrame(AbstractInsnNode insn) {
    int index = methodAdapter.instructions.indexOf(insn);
    FlowFrame frame = (FlowFrame) getFrames()[index];
    if (frame.getInsn() != insn) {
      throw new IllegalStateException("Instruction and frame index don't match");
    }
    return frame;
  }
}
