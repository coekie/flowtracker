package be.coekaerts.wouter.flowtracker.weaver.flow;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** A char received from a {@link String#charAt(int)} call. */
class CharAtValue extends TrackableValue {
  /** The charAt() call */
  private final MethodInsnNode mInsn;

  /** Index of the local variable storing the target String */
  private int targetStringLocal;
  /** Index of the local variable storing the index */
  private int indexLocal;

  CharAtValue(MethodInsnNode mInsn) {
    super(Type.CHAR_TYPE);
    this.mInsn = mInsn;
    // Note: when it can return something else than char, we get type with Type.getReturnType(mInsn.desc)
  }

  @Override void insertTrackStatements(MethodNode methodNode) {
    // on the stack before the charAt call: String target, int index

    targetStringLocal = methodNode.maxLocals++;
    indexLocal = methodNode.maxLocals++;

    InsnList toInsert = new InsnList();

    // we could avoid some local-to-stack copying by dupping:
    // toInsert.add(new InsnNode(Opcodes.DUP2));

    toInsert.add(new VarInsnNode(Opcodes.ISTORE, indexLocal));
    toInsert.add(new VarInsnNode(Opcodes.ASTORE, targetStringLocal));

    toInsert.add(new VarInsnNode(Opcodes.ALOAD, targetStringLocal));
    toInsert.add(new VarInsnNode(Opcodes.ILOAD, indexLocal));

    methodNode.instructions.insertBefore(mInsn, toInsert);
  }

  @Override void loadSourceTracker(InsnList toInsert) {
    // insert code for: StringHook.getStringTrack(targetString);
    toInsert.add(new VarInsnNode(Opcodes.ALOAD, targetStringLocal));
    Method getStringTrack = Method.getMethod(
        "be.coekaerts.wouter.flowtracker.tracker.PartTracker getStringTrack(String)");
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC, "be/coekaerts/wouter/flowtracker/hook/StringHook",
            getStringTrack.getName(), getStringTrack.getDescriptor(), false));
  }

  @Override void loadSourceIndex(InsnList toInsert) {
    toInsert.add(new VarInsnNode(Opcodes.ILOAD, indexLocal));
  }
}
