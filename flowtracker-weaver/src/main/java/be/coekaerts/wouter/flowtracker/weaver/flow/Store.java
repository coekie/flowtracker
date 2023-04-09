package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.tree.analysis.Frame;

/** Represents an instruction that stores a (possibly tracked) value in an object. */
abstract class Store {
  final Frame<FlowValue> frame;

  Store(Frame<FlowValue> frame) {
    this.frame = frame;
  }

  abstract void insertTrackStatements(FlowMethodAdapter methodNode);

  FlowValue getStackFromTop(int indexFromTop) {
    return frame.getStack(frame.getStackSize() - indexFromTop - 1);
  }
}
