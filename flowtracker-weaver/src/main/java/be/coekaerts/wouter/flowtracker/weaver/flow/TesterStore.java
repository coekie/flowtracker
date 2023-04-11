package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Frame;

/**
 * Invocation of a method on FlowTester that receives a tracked value.
 * For testing only.
 */
// on the stack: value, ...
class TesterStore extends Store {
  private final MethodInsnNode invokeInsn;
  private final int valueStackIndexFromTop;

  TesterStore(MethodInsnNode invokeInsn, Frame<FlowValue> frame, int valueStackIndexFromTop) {
    super(frame);
    this.invokeInsn = invokeInsn;
    this.valueStackIndexFromTop = valueStackIndexFromTop;
  }

  void insertTrackStatements(FlowMethodAdapter methodNode) {
    FlowValue stored = getStackFromTop(valueStackIndexFromTop);

    if (stored.isTrackable()) { // if we know where the value we are storing came from
      stored.ensureTracked();

      // replace the call with a call to the $tracked_ method, with two extra arguments: the tracker
      // and the index
      InsnList toInsert = new InsnList();
      methodNode.addComment(toInsert, "begin TesterStore.insertTrackStatements");

      stored.loadSourceTracker(toInsert);
      stored.loadSourceIndex(toInsert);

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
