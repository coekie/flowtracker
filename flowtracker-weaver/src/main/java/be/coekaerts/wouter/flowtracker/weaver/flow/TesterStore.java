package be.coekaerts.wouter.flowtracker.weaver.flow;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

/**
 * Invocation of a method on FlowTester that receives a tracked value.
 * For testing only.
 */
// on the stack: value, ...
class TesterStore extends Store {
  private final MethodInsnNode invokeInsn;
  private final int valueStackIndexFromTop;

  TesterStore(MethodInsnNode invokeInsn, Frame<BasicValue> frame, int valueStackIndexFromTop) {
    super(frame);
    this.invokeInsn = invokeInsn;
    this.valueStackIndexFromTop = valueStackIndexFromTop;
  }

  void insertTrackStatements(MethodNode methodNode) {
    BasicValue stored = getStackFromTop(valueStackIndexFromTop);

    if (stored instanceof TrackableValue) { // if we know where the value we are storing came from
      TrackableValue trackedStored = (TrackableValue) stored;
      trackedStored.ensureTracked(methodNode);

      // replace the call with a call to the $tracked_ method, with two extra arguments: the tracker
      // and the index
      InsnList toInsert = new InsnList();
      trackedStored.loadSourceTracker(toInsert);
      trackedStored.loadSourceIndex(toInsert);

      methodNode.maxStack = Math.max(frame.getStackSize() + 3, methodNode.maxStack);

      invokeInsn.name = "$tracked_" + invokeInsn.name;
      invokeInsn.desc =
          invokeInsn.desc.replace(")", "Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;I)");

      methodNode.instructions.insertBefore(invokeInsn, toInsert);
    }
  }
}
