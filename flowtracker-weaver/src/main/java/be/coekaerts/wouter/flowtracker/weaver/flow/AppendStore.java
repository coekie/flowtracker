package be.coekaerts.wouter.flowtracker.weaver.flow;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

/** Invocation of {@link StringBuilder#append(char)} or {@link StringBuffer#append(char)}. */
// on the stack: StringBuffer/StringBuilder target, char c
class AppendStore extends Store {
  private final MethodInsnNode invokeInsn;

  AppendStore(MethodInsnNode invokeInsn, Frame<BasicValue> frame) {
    super(frame);
    this.invokeInsn = invokeInsn;
  }

  void insertTrackStatements(MethodNode methodNode) {
    BasicValue stored = getCharValue();

    InsnList toInsert = new InsnList();

    if (stored instanceof TrackableValue) { // if we know where the value we are storing came from
      TrackableValue trackedStored = (TrackableValue) stored;
      trackedStored.ensureTracked(methodNode);

      trackedStored.loadSourceTracker(toInsert);
      trackedStored.loadSourceIndex(toInsert);

      methodNode.maxStack = Math.max(frame.getStackSize() + 2, methodNode.maxStack);

      // invokeInsn.name is either java/lang/StringBuilder or java/lang/StringBuffer
      String hookMethod;
      if ("java/lang/StringBuilder".equals(invokeInsn.owner)) {
        hookMethod = "(Ljava/lang/StringBuilder;CLbe/coekaerts/wouter/flowtracker/tracker/Tracker;I)Ljava/lang/StringBuilder;";
      } else if ("java/lang/StringBuffer".equals(invokeInsn.owner)) {
        hookMethod = "(Ljava/lang/StringBuffer;CLbe/coekaerts/wouter/flowtracker/tracker/Tracker;I)Ljava/lang/StringBuffer;";
      } else {
        throw new IllegalStateException("Should not be here for append in " + invokeInsn.owner);
      }
      toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "be/coekaerts/wouter/flowtracker/hook/AbstractStringBuilderHook", "append",
          hookMethod,
          false));

      methodNode.instructions.insert(invokeInsn, toInsert);
      methodNode.instructions.remove(invokeInsn); // our hook takes care of the storing
    }
  }

  /** The value being stored */
  private BasicValue getCharValue() {
    return getStackFromTop(0);
  }
}
