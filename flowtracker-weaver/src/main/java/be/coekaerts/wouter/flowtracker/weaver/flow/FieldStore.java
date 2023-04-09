package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.FieldRepository;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Frame;

/** The storing of a value in a field. */
// on the stack: Object target, [int|char|byte] value
class FieldStore extends Store {
  private final FieldInsnNode storeInsn;

  FieldStore(FieldInsnNode storeInsn, Frame<FlowValue> frame) {
    super(frame);
    this.storeInsn = storeInsn;
  }

  void insertTrackStatements(FlowMethodAdapter methodNode) {
    FlowValue stored = getStackFromTop(0);

    // only track char or byte
    if (!FieldValue.shouldTrackType(stored.getType())) {
      return;
    }

    InsnList toInsert = new InsnList();

    methodNode.addComment(toInsert, "begin FieldStore.insertTrackStatements");
    // put a copy on top of the stack to pass as argument into the hook method, in two steps:
    // starting with (target, value) on the stack,
    // dup2 -> (target, value, target, value),
    toInsert.add(new InsnNode(Opcodes.DUP2));
    // pop -> (target, value, target).
    toInsert.add(new InsnNode(Opcodes.POP));
    toInsert.add(new LdcInsnNode(FieldRepository.fieldId(storeInsn.owner, storeInsn.name)));

    if (stored instanceof TrackableValue) { // if we know where the value we are storing came from
      TrackableValue trackedStored = (TrackableValue) stored;
      trackedStored.ensureTracked();

      trackedStored.loadSourceTracker(toInsert);
      trackedStored.loadSourceIndex(toInsert);
    } else {
      toInsert.add(new InsnNode(Opcodes.ACONST_NULL));
      toInsert.add(new InsnNode(Opcodes.ICONST_0));
    }

    methodNode.maxStack = Math.max(frame.getStackSize() + 5, methodNode.maxStack);

    toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
        "be/coekaerts/wouter/flowtracker/tracker/FieldRepository", "setPoint",
        "(Ljava/lang/Object;Ljava/lang/String;Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;I)V",
        false));

    methodNode.addComment(toInsert, "end FieldStore.insertTrackStatements");

    methodNode.instructions.insertBefore(storeInsn, toInsert);
  }
}
