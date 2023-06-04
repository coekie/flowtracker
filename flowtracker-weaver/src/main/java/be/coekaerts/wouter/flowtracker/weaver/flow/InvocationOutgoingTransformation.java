package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Manages transformation of a method call for which we may track the return value and/or arguments.
 *
 * @see InvocationArgStore
 * @see InvocationReturnValue
 * @see InvocationIncomingTransformation
 */
class InvocationOutgoingTransformation {
  private final MethodInsnNode mInsn;
  private final FlowMethodAdapter methodNode;
  private AbstractInsnNode endInsn;

  InvocationOutgoingTransformation(MethodInsnNode mInsn, FlowMethodAdapter methodNode) {
    this.mInsn = mInsn;
    this.methodNode = methodNode;
  }

  /** Insert call to {@link Invocation#calling(String)} if it hasn't been added yet */
  void ensureInstrumented() {
    if (endInsn != null) {
      return;
    }

    InsnList toInsert = new InsnList();
    methodNode.addComment(toInsert, "begin InvocationOutgoingTransformation.ensureInstrumented");
    toInsert.add(new LdcInsnNode(Invocation.signature(mInsn.name, mInsn.desc)));
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/tracker/Invocation",
            "calling",
            "(Ljava/lang/String;)Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;"));
    // initially we assume we're not going to need the Invocation anymore.
    endInsn = new InsnNode(Opcodes.POP);
    toInsert.add(endInsn);

    methodNode.addComment(toInsert, "end InvocationOutgoingTransformation.ensureInstrumented");
    methodNode.instructions.insertBefore(mInsn, toInsert);
  }

  /**
   * Insert instructions that operate on the Invocation, before the actual method invocation. At
   * this point the {@link Invocation} is on top of the stack, and at the end of the inserted
   * instructions it should still be there.
   */
  void insertInvocationPreparation(InsnList toInsert) {
    methodNode.instructions.insertBefore(endInsn, toInsert);
  }

  /**
   * Instead for POP'ing the {@link Invocation} at the end, store it in the given local var.
   */
  void storeInvocation(TrackLocal invocationLocal) {
    if (endInsn == null || endInsn.getOpcode() != Opcodes.POP) {
      throw new IllegalStateException();
    }
    VarInsnNode newEndInsn = invocationLocal.store();
    methodNode.instructions.set(endInsn, newEndInsn);
    endInsn = newEndInsn;
  }
}
