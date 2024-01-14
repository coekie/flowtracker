package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * The passing of a value as argument into the invocation of another method that may be instrumented
 * with {@link Invocation}.
 */
public class InvocationArgStore extends Store {
  // for now, we only support calls with one argument
  private final FlowValue arg0 = getStackFromTop(0);
  private final InvocationOutgoingTransformation transformation;

  InvocationArgStore(MethodInsnNode mInsn, FlowFrame frame, FlowFrame nextFrame) {
    super(frame);
    this.transformation = getOrCreateOutgoingTransformation(mInsn, frame, nextFrame);
  }

  /**
   * Get the {@link InvocationOutgoingTransformation} created by the {@link InvocationReturnValue}
   * for the same call if it exists; otherwise create a new one.
   */
  private static InvocationOutgoingTransformation getOrCreateOutgoingTransformation(
      MethodInsnNode mInsn, FlowFrame frame, FlowFrame nextFrame) {
    if (nextFrame != null && !mInsn.desc.endsWith(")V")) {
      FlowValue value = nextFrame.getStack(nextFrame.getStackSize() - 1);
      if (value instanceof InvocationReturnValue) {
        InvocationReturnValue invocationReturnValue = ((InvocationReturnValue) value);
        return invocationReturnValue.transformation;
      }
    }
    return new InvocationOutgoingTransformation(mInsn, frame.getFlowMethodAdapter());
  }

  @Override
  void insertTrackStatements(FlowMethodAdapter methodNode) {
    // if we know where the value passed in as argument came from
    if (arg0.isTrackable()) {
      arg0.ensureTracked();

      // we add these instructions using insertInvocationPreparation, so Invocation is on top of the
      // stack. setArg0 returns the Invocation, so it's on top of the stack again.
      InsnList toInsert = new InsnList();
      methodNode.addComment(toInsert, "begin InvocationArgStore.insertTrackStatements");
      arg0.loadSourceTracker(toInsert);
      arg0.loadSourceIndex(toInsert);
      toInsert.add(
          new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
              "be/coekaerts/wouter/flowtracker/tracker/Invocation",
              "setArg0",
              "(Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;I)"
                  + "Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;"));
      methodNode.addComment(toInsert, "end InvocationArgStore.insertTrackStatements");

      transformation.ensureInstrumented();
      transformation.insertInvocationPreparation(toInsert);

      methodNode.maxStack = Math.max(frame.getStackSize() + 3, methodNode.maxStack);
    }
  }

  static boolean shouldInstrumentInvocationArg(String name, String desc) {
    Type[] args = Type.getArgumentTypes(desc);
    if (args.length != 1) {
      return false;
    }

    if (args[0].equals(Type.CHAR_TYPE) || args[0].equals(Type.BYTE_TYPE)) {
      return true;
    }

    return args[0].equals(Type.INT_TYPE)
        && (name.contains("write") || name.contains("Write") || name.contains("print"));
  }
}
