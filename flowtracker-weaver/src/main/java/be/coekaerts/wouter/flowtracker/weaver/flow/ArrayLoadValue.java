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

  /** Local variable storing the PointTracker for the loaded char or byte */
  private TrackLocal pointTrackerLocal;

  ArrayLoadValue(FlowMethodAdapter flowMethodAdapter, InsnNode insn, Type type) {
    super(flowMethodAdapter, type, insn);
    this.insn = insn;
  }

  @Override void insertTrackStatements() {
    // on the stack before the CALOAD call: char[] target, int index

    pointTrackerLocal = flowMethodAdapter.newLocalForObject(
        Type.getType("Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;"),
        "ArrayLoadValue PointTracker");

    InsnList toInsert = new InsnList();
    flowMethodAdapter.addComment(toInsert, "begin ArrayLoadValue.insertTrackStatements");

    // insert code for: pointTracker = ArrayLoadHook.getElementTracker(target, index)
    // use DUP2 to copy target and index for getElementTracker while leaving it on the stack for the
    // actual CALOAD
    toInsert.add(new InsnNode(Opcodes.DUP2));
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/hook/ArrayLoadHook",
            "getElementTracker",
            "(Ljava/lang/Object;I)Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;",
            false));
    toInsert.add(pointTrackerLocal.store());
    flowMethodAdapter.maxStack = Math.max(flowMethodAdapter.maxStack,
        getCreationFrame().getStackSize() + 2);

    flowMethodAdapter.addComment(toInsert, "end ArrayLoadValue.insertTrackStatements");

    flowMethodAdapter.instructions.insertBefore(insn, toInsert);
  }

  @Override void loadSourceTracker(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert,
        "ArrayLoadValue.loadSourceTracker: PointTracker.getTracker(pointTracker)");
    doLoadSourceTrackerFromPoint(toInsert, pointTrackerLocal);
  }

  @Override void loadSourceIndex(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert,
        "ArrayLoadValue.loadSourceIndex: PointTracker.getIndex(pointTracker)");
    doLoadSourceIndexFromPoint(toInsert, pointTrackerLocal);
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert, "ArrayLoadValue.loadSourcePoint");
    toInsert.add(pointTrackerLocal.load());
  }
}
