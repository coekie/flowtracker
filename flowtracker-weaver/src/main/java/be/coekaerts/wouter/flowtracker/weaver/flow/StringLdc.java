package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Handles instrumentation of an LDC instruction that loads a String.
 * <p>
 * This isn't really a "store", it's not storing a value, but the instrumentation works the same as
 * instrumentation of storing of values in {@link FlowAnalyzingTransformer}, so this extends
 * {@link Store} to make that work.
 * (Perhaps we should rename "Store", or extract a common superclass for real Stores and this).
 */
public class StringLdc extends Store {
  private final LdcInsnNode insn;

  StringLdc(LdcInsnNode insn, FlowFrame frame) {
    super(frame);
    this.insn = insn;
  }

  @Override
  void insertTrackStatements(FlowMethodAdapter methodNode) {
    ConstantsTransformation constantsTransformation = methodNode.constantsTransformation;
    if (constantsTransformation.canBreakStringInterning(methodNode)) {
      String value = (String) insn.cst;
      int offset = constantsTransformation.trackConstantString(methodNode, value);
      // we prefer to use constant-dynamic, that is replacing just the LDC value. That is better for
      // performance, and at least partially maintains the properties of String interning: the
      // literal String loaded from the same location in the code will still always be the same
      // instance.
      // When necessary we fall back to replacing the LDC with a call to ConstantHook. (If we wanted
      // to then we could make a ConstantHook.constantPoint variant that still always return the
      // same instance, but we just haven't implemented that.)
      if (methodNode.canUseConstantDynamic()) {
        insn.cst = constantsTransformation.stringConstantDynamic(offset, value);
      } else {
        InsnList toInsert = new InsnList();
        methodNode.addComment(toInsert, "begin StringLdc.insertTrackStatements");
        toInsert.add(ConstantsTransformation.iconst(constantsTransformation.classId()));
        toInsert.add(ConstantsTransformation.iconst(offset));
        toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/hook/StringHook", "constantString",
            "(Ljava/lang/String;II)Ljava/lang/String;"));
        methodNode.addComment(toInsert, "end StringLdc.insertTrackStatements");
        methodNode.instructions.insert(insn, toInsert);
        methodNode.maxStack = Math.max(frame.fullStackSize() + 3, methodNode.maxStack);
      }
    }
  }
}
