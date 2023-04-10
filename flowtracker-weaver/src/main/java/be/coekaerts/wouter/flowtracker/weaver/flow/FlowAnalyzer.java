package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.tree.analysis.Analyzer;
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
}
