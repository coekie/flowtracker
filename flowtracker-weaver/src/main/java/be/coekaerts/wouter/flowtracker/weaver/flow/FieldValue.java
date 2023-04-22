package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.FieldRepository;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** A value from getting a field value ({@link Opcodes#GETFIELD}) */
class FieldValue extends TrackableValue {
  /** The {@link Opcodes#GETFIELD} call */
  private final FieldInsnNode insn;

  /** Local variable storing the PointTracker for the loaded char or byte */
  private TrackLocal pointTrackerLocal;

  FieldValue(FlowMethodAdapter flowMethodAdapter, FieldInsnNode insn, Type type) {
    super(flowMethodAdapter, type, insn);
    this.insn = insn;
  }

  @Override void insertTrackStatements() {
    // on the stack before the GETFIELD call: target

    pointTrackerLocal = flowMethodAdapter.newLocalForObject(
        Type.getType("Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;"),
        "FieldValue PointTracker");

    InsnList toInsert = new InsnList();
    flowMethodAdapter.addComment(toInsert, "begin FieldValue.insertTrackStatements");

    // insert code for: pointTracker = FieldRepository.getPoint(target, fieldId)
    // use DUP to copy target for getPoint while leaving it on the stack for the actual GETFIELD
    toInsert.add(new InsnNode(Opcodes.DUP));
    toInsert.add(new LdcInsnNode(FieldRepository.fieldId(insn.owner, insn.name)));
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/tracker/FieldRepository",
            "getPoint",
            "(Ljava/lang/Object;Ljava/lang/String;)"
                + "Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;",
            false));
    toInsert.add(pointTrackerLocal.store());

    flowMethodAdapter.maxStack = Math.max(flowMethodAdapter.maxStack,
        getCreationFrame().getStackSize() + 2);

    flowMethodAdapter.addComment(toInsert, "end FieldValue.insertTrackStatements");

    flowMethodAdapter.instructions.insertBefore(insn, toInsert);
  }

  @Override void loadSourceTracker(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert,
        "FieldValue.loadSourceTracker: PointTracker.getTracker(pointTracker)");
    doLoadSourceTrackerFromPoint(toInsert,  pointTrackerLocal);
  }

  @Override void loadSourceIndex(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert,
        "FieldValue.loadSourceIndex: PointTracker.getIndex(pointTracker)");
    doLoadSourceIndexFromPoint(toInsert, pointTrackerLocal);
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert, "FieldValue.loadSourcePoint");
    toInsert.add(pointTrackerLocal.load());
  }

  static boolean shouldTrackType(Type type) {
    if (type == null) {
      return false;
    }
    int sort = type.getSort();
    return sort == Type.CHAR || sort == Type.BYTE;
  }
}
