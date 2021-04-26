package be.coekaerts.wouter.flowtracker.weaver.flow;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** A value received from a {@code FlowTester.createSource*()}. */
class TesterValue extends TrackableValue {
  /** The call to FlowTester.createSource*() */
  private final MethodInsnNode mInsn;

  /** Index of the local variable storing the target FlowTester */
  private int testerLocal;

  TesterValue(MethodInsnNode mInsn) {
    super(Type.getReturnType(mInsn.desc));
    this.mInsn = mInsn;
  }

  @Override void insertTrackStatements(MethodNode methodNode) {
    // on the stack before the call: FlowTester tester, char c
    testerLocal = methodNode.maxLocals++;

    InsnList toInsert = new InsnList();

    // store tester
    toInsert.add(new InsnNode(Opcodes.DUP2)); // dup tester and c
    toInsert.add(new InsnNode(Opcodes.POP)); // pop c
    toInsert.add(new VarInsnNode(Opcodes.ASTORE, testerLocal)); // store tester

    mInsn.name = "$tracked_" + mInsn.name;

    methodNode.instructions.insertBefore(mInsn, toInsert);
  }

  @Override void loadSourceTracker(InsnList toInsert) {
    // insert code for: testerLocal.theSource()
    toInsert.add(new VarInsnNode(Opcodes.ALOAD, testerLocal));
      toInsert.add(
          new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
              "be/coekaerts/wouter/flowtracker/test/FlowTester",
              "theSource",
              "()Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;",
              false));
  }

  @Override void loadSourceIndex(InsnList toInsert) {
    toInsert.add(new VarInsnNode(Opcodes.ALOAD, testerLocal));
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
            "be/coekaerts/wouter/flowtracker/test/FlowTester",
            "theSourceIndex",
            "()I",
            false));
  }
}
