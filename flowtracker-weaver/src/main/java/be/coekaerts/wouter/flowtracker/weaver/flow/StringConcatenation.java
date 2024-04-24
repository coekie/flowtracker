package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

/**
 * Handles instrumentation of an invokedynamic instruction to StringConcatFactory.
 *
 * @see StringLdc
 */
public class StringConcatenation extends Store {
  private final InvokeDynamicInsnNode insn;

  StringConcatenation(InvokeDynamicInsnNode insn, FlowFrame frame) {
    super(frame);
    this.insn = insn;
  }

  @Override
  void insertTrackStatements(FlowMethodAdapter methodNode) {
    ConstantsTransformation constantsTransformation = methodNode.constantsTransformation;

    // StringConcatFactory was introduced in jdk 9, condy in 11. for simplicity, we don't handle
    // tracking of string concatenation for classes compiled with jdk 9 or 10 (with
    // StringConcatFactory but without condy available).
    // just like in StringLdc, we replace the String constants being loaded with tracked Strings.
    // note: this isn't even StringConcatFactory-specific, if we wanted we could do this for all
    // invokedynamic instructions. but I haven't seen an example yet of where that's useful.
    if (methodNode.canUseConstantDynamic()) {
      Object[] bsmArgs = insn.bsmArgs;
      for (int i = 0; i < bsmArgs.length; i++) {
        if (bsmArgs[i] instanceof String) {
          String argValue = (String) bsmArgs[i];
          int offset = constantsTransformation.trackConstantString(methodNode, argValue);
          bsmArgs[i] = constantsTransformation.stringConstantDynamic(offset, argValue);
        }
      }
    }
  }
}
