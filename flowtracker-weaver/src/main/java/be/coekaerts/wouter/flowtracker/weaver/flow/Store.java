package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

/** Represents an instruction that stores a (possibly tracked) value in an object. */
abstract class Store {
  final Frame<BasicValue> frame;

  Store(Frame<BasicValue> frame) {
    this.frame = frame;
  }

  abstract void insertTrackStatements(FlowMethodAdapter methodNode);

  BasicValue getStackFromTop(int indexFromTop) {
    return frame.getStack(frame.getStackSize() - indexFromTop - 1);
  }
}
