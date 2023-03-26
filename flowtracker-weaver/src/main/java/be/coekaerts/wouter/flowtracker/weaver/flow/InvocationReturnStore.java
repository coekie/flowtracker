package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

/** The returning of a value of a method that may be instrumented with {@link Invocation} */
public class InvocationReturnStore extends Store {
  private final InsnNode returnInsn;
  private final InvocationTransformation invocation;

  InvocationReturnStore(InsnNode returnInsn, Frame<BasicValue> frame,
      InvocationTransformation invocation) {
    super(frame);
    this.returnInsn = returnInsn;
    this.invocation = invocation;
  }

  @Override
  void insertTrackStatements(FlowMethodAdapter methodNode) {
    BasicValue returnedValue = getStackFromTop(0);

    // if we know where the value we are returning came from
    if (returnedValue instanceof TrackableValue) {
      InsnList toInsert = new InsnList();
      methodNode.addComment(toInsert, "begin InvocationReturnStore.insertTrackStatements");

      TrackableValue trackableReturnValue = (TrackableValue) returnedValue;
      trackableReturnValue.ensureTracked();
      invocation.ensureStarted(methodNode);

      toInsert.add(invocation.invocationLocal.load());
      trackableReturnValue.loadSourceTracker(toInsert);
      trackableReturnValue.loadSourceIndex(toInsert);

      toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "be/coekaerts/wouter/flowtracker/tracker/Invocation", "returning",
          "(Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;"
              + "Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;I)V",
          false));

      methodNode.addComment(toInsert, "end InvocationReturnStore.insertTrackStatements");
      methodNode.maxStack = Math.max(frame.getStackSize() + 3, methodNode.maxStack);

      methodNode.instructions.insertBefore(returnInsn, toInsert);
    }
  }
}
