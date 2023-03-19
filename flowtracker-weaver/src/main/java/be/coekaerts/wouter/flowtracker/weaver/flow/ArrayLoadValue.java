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

  /** Local variable storing the PointTracker for the loaded char or byte */
  private TrackLocal pointTrackerLocal;

  ArrayLoadValue(InsnNode insn, Type type, Type arrayType) {
    super(type);
    this.insn = insn;
    this.arrayType = arrayType;
  }

  @Override void insertTrackStatements(FlowMethodAdapter methodNode) {
    // on the stack before the CALOAD call: char[] target, int index

    // Local variable storing the target char[] or byte[]
    TrackLocal targetLocal = methodNode.newLocalForObject(arrayType);
    // Local variable storing the index
    TrackLocal indexLocal = methodNode.newLocalForIndex();
    pointTrackerLocal = methodNode.newLocalForObject(
        Type.getType("Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;"));

    InsnList toInsert = new InsnList();

    // store target and index
    toInsert.add(indexLocal.store());
    toInsert.add(targetLocal.store());

    // insert code for: pointTracker = ArrayLoadHook.getElementTracker(target, index)
    toInsert.add(targetLocal.load());
    toInsert.add(indexLocal.load());
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/hook/ArrayLoadHook",
            "getElementTracker",
            "(Ljava/lang/Object;I)Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;",
            false));
    toInsert.add(pointTrackerLocal.store());

    // put target and index back on the stack for the actual operation
    toInsert.add(targetLocal.load());
    toInsert.add(indexLocal.load());

    methodNode.instructions.insertBefore(insn, toInsert);
  }

  @Override void loadSourceTracker(InsnList toInsert) {
    // insert code for: PointTracker.getTracker(pointTracker)
    toInsert.add(pointTrackerLocal.load());
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/tracker/TrackerPoint",
            "getTracker",
            "(Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;)Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;",
            false));
  }

  @Override void loadSourceIndex(InsnList toInsert) {
    // insert code for: PointTracker.getIndex(pointTracker)
    toInsert.add(pointTrackerLocal.load());
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/tracker/TrackerPoint",
            "getIndex",
            "(Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;)I",
            false));
  }
}
