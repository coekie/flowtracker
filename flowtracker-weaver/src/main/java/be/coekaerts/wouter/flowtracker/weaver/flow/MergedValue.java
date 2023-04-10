package be.coekaerts.wouter.flowtracker.weaver.flow;

import org.objectweb.asm.Type;

/**
 * A value that can come from more than one source due to control flow (e.g. due to if-statements or
 * ternary operator).
 */
public class MergedValue extends FlowValue {
  final FlowFrame mergingFrame;

  MergedValue(Type type, FlowFrame mergingFrame) {
    super(type);
    this.mergingFrame = mergingFrame;
  }
}
