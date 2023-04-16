package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Invocation of a method on FlowTester that receives a tracked value.
 * For testing only.
 */
// on the stack: value, ...
class TesterStore extends Store {
  private final MethodInsnNode invokeInsn;
  private final FlowValue storedValue;

  TesterStore(MethodInsnNode invokeInsn, FlowFrame frame, int valueStackIndexFromTop) {
    super(frame);
    this.invokeInsn = invokeInsn;
    this.storedValue = getStackFromTop(valueStackIndexFromTop);
  }

  void insertTrackStatements(FlowMethodAdapter methodNode) {
    if (storedValue.isTrackable()) { // if we know where the value we are storing came from
      storedValue.ensureTracked();

      // replace the call with a call to the $tracked_ method, with two extra arguments: the tracker
      // and the index
      InsnList toInsert = new InsnList();
      methodNode.addComment(toInsert, "begin TesterStore.insertTrackStatements");

      storedValue.loadSourceTracker(toInsert);
      storedValue.loadSourceIndex(toInsert);

      methodNode.addComment(toInsert,
          "end TesterStore.insertTrackStatements. also replaced next invocation");

      methodNode.maxStack = Math.max(frame.getStackSize() + 3, methodNode.maxStack);

      invokeInsn.name = "$tracked_" + invokeInsn.name;
      invokeInsn.desc =
          invokeInsn.desc.replace(")", "Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;I)");

      methodNode.instructions.insertBefore(invokeInsn, toInsert);
    }
  }
}
