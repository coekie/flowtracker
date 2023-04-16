package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

/** Invocation of {@link StringBuilder#append(char)} or {@link StringBuffer#append(char)}. */
// on the stack: StringBuffer/StringBuilder target, char c
class AppendStore extends Store {
  private final MethodInsnNode invokeInsn;
  private final FlowValue storedValue = getStackFromTop(0);

  AppendStore(MethodInsnNode invokeInsn, FlowFrame frame) {
    super(frame);
    this.invokeInsn = invokeInsn;
  }

  void insertTrackStatements(FlowMethodAdapter methodNode) {
    if (storedValue.isTrackable()) { // if we know where the value we are storing came from
      InsnList toInsert = new InsnList();
      methodNode.addComment(toInsert, "begin AppendStore.insertTrackStatements: "
          + "AbstractStringBuilderHook.append(sb, c [already on stack], tracker, index");

      storedValue.ensureTracked();
      storedValue.loadSourceTracker(toInsert);
      storedValue.loadSourceIndex(toInsert);

      methodNode.maxStack = Math.max(frame.getStackSize() + 2, methodNode.maxStack);

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

      methodNode.addComment(toInsert, "end AppendStore.insertTrackStatements");

      methodNode.instructions.insert(invokeInsn, toInsert);
      methodNode.instructions.remove(invokeInsn); // our hook takes care of the storing
    }
  }
}
