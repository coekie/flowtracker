package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** A value received from a {@code FlowTester.createSource*()}. */
class TesterValue extends TrackableValue {
  /** The call to FlowTester.createSource*() */
  private final MethodInsnNode mInsn;

  /** Local variable storing the target FlowTester */
  private TrackLocal testerLocal;

  TesterValue(MethodInsnNode mInsn) {
    super(Type.getReturnType(mInsn.desc));
    this.mInsn = mInsn;
  }

  @Override void insertTrackStatements(FlowMethodAdapter methodNode) {
    // on the stack before the call: FlowTester tester, char c
    testerLocal = methodNode.newLocalForObject(
        Type.getObjectType("be/coekaerts/wouter/flowtracker/test/FlowTester"));

    InsnList toInsert = new InsnList();

    // store tester
    toInsert.add(new InsnNode(Opcodes.DUP2)); // dup tester and c
    toInsert.add(new InsnNode(Opcodes.POP)); // pop c
    toInsert.add(testerLocal.store()); // store tester

    mInsn.name = "$tracked_" + mInsn.name;

    methodNode.instructions.insertBefore(mInsn, toInsert);
  }

  @Override void loadSourceTracker(InsnList toInsert) {
    // insert code for: testerLocal.theSource()
    toInsert.add(testerLocal.load());
      toInsert.add(
          new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
              "be/coekaerts/wouter/flowtracker/test/FlowTester",
              "theSource",
              "()Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;",
              false));
  }

  @Override void loadSourceIndex(InsnList toInsert) {
    toInsert.add(testerLocal.load());
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
            "be/coekaerts/wouter/flowtracker/test/FlowTester",
            "theSourceIndex",
            "()I",
            false));
  }
}
