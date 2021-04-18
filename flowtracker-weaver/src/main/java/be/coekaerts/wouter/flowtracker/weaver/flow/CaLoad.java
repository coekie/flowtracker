package be.coekaerts.wouter.flowtracker.weaver.flow;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** A value from a {@link Opcodes#CALOAD} operation: the loading of a char from a char[]. */
// on the stack: char[] target, int index
class CaLoad extends TrackableValue {
  /** The {@link Opcodes#CALOAD} call */
  private final InsnNode insn;

  /** Index of the local variable storing the target char[] */
  private int targetLocal;
  /** Index of the local variable storing the index */
  private int indexLocal;

  CaLoad(InsnNode insn) {
    super(Type.CHAR_TYPE);
    this.insn = insn;
  }

  @Override void insertTrackStatements(MethodNode methodNode) {
    // on the stack before the CALOAD call: char[] target, int index

    targetLocal = methodNode.maxLocals++;
    indexLocal = methodNode.maxLocals++;

    InsnList toInsert = new InsnList();

    // store target and index
    toInsert.add(new VarInsnNode(Opcodes.ISTORE, indexLocal));
    toInsert.add(new VarInsnNode(Opcodes.ASTORE, targetLocal));

    // put them back on the stack for the actual operation
    toInsert.add(new VarInsnNode(Opcodes.ALOAD, targetLocal));
    toInsert.add(new VarInsnNode(Opcodes.ILOAD, indexLocal));

    methodNode.instructions.insertBefore(insn, toInsert);
  }

  @Override void loadSourceTracker(InsnList toInsert) {
    // insert code for: TrackerRepository.getTracker(target);
    toInsert.add(new VarInsnNode(Opcodes.ALOAD, targetLocal));
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/tracker/TrackerRepository",
            "getTracker",
            "(Ljava/lang/Object;)Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;",
            false));
  }

  @Override void loadSourceIndex(InsnList toInsert) {
    toInsert.add(new VarInsnNode(Opcodes.ILOAD, indexLocal));
  }
}
