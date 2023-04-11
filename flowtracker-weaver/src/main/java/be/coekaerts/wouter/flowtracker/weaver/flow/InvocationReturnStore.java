package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.Frame;

/** The returning of a value of a method that may be instrumented with {@link Invocation} */
public class InvocationReturnStore extends Store {
  private final InsnNode returnInsn;
  private final InvocationTransformation invocation;

  InvocationReturnStore(InsnNode returnInsn, Frame<FlowValue> frame,
      InvocationTransformation invocation) {
    super(frame);
    this.returnInsn = returnInsn;
    this.invocation = invocation;
  }

  @Override
  void insertTrackStatements(FlowMethodAdapter methodNode) {
    FlowValue returnedValue = getStackFromTop(0);

    // if we know where the value we are returning came from
    if (returnedValue.isTrackable()) {
      InsnList toInsert = new InsnList();
      methodNode.addComment(toInsert, "begin InvocationReturnStore.insertTrackStatements");

      returnedValue.ensureTracked();
      invocation.ensureStarted(methodNode);

      toInsert.add(invocation.invocationLocal.load());
      returnedValue.loadSourceTracker(toInsert);
      returnedValue.loadSourceIndex(toInsert);

      toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "be/coekaerts/wouter/flowtracker/tracker/Invocation", "returning",
          "(Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;"
              + "Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;I)V",
          false));

      methodNode.addComment(toInsert, "end InvocationReturnStore.insertTrackStatements");
      methodNode.maxStack = Math.max(frame.getStackSize() + 3, methodNode.maxStack);

      methodNode.instructions.insertBefore(returnInsn, toInsert);
    } else if (returnedValue instanceof MergedValue) {
      // TODO this is just temporary for testing
      MergedValue mergedValue = (MergedValue) returnedValue;
      InsnList toInsert = new InsnList();
      methodNode.addComment(toInsert, "Here is the MergedValue's merging");
      methodNode.instructions.insertBefore(mergedValue.mergingFrame.getInsn(), toInsert);
    }
  }
}
