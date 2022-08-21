package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** A value from getting an element of an array (e.g. {@link Opcodes#CALOAD}) */
// on the stack: char[]/byte[] target, int index
class ArrayLoadValue extends TrackableValue {
  /** The {@link Opcodes#CALOAD} call */
  private final InsnNode insn;
  private final Type arrayType;

  /** Local variable storing the target char[] or byte[] */
  private TrackLocal targetLocal;
  /** Local variable storing the index */
  private TrackLocal indexLocal;

  ArrayLoadValue(InsnNode insn, Type type, Type arrayType) {
    super(type);
    this.insn = insn;
    this.arrayType = arrayType;
  }

  @Override void insertTrackStatements(FlowMethodAdapter methodNode) {
    // on the stack before the CALOAD call: char[] target, int index

    targetLocal = methodNode.newLocalForObject(arrayType);
    indexLocal = methodNode.newLocalForIndex();

    InsnList toInsert = new InsnList();

    // store target and index
    toInsert.add(indexLocal.store());
    toInsert.add(targetLocal.store());

    // put them back on the stack for the actual operation
    toInsert.add(targetLocal.load());
    toInsert.add(indexLocal.load());

    methodNode.instructions.insertBefore(insn, toInsert);
  }

  @Override void loadSourceTracker(InsnList toInsert) {
    // insert code for: TrackerRepository.getTracker(target);
    toInsert.add(targetLocal.load());
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/tracker/TrackerRepository",
            "getTracker",
            "(Ljava/lang/Object;)Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;",
            false));
  }

  @Override void loadSourceIndex(InsnList toInsert) {
    toInsert.add(indexLocal.load());
  }
}
