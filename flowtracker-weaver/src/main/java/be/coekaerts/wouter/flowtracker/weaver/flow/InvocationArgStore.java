package be.coekaerts.wouter.flowtracker.weaver.flow;

import static java.util.Objects.requireNonNull;

import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * The passing of a value as argument into the invocation of another method that may be instrumented
 * with {@link Invocation}.
 */
public class InvocationArgStore extends Store {
  // for now, we only support calls with one argument
  private final FlowValue[] args;
  private final InvocationOutgoingTransformation transformation;

  InvocationArgStore(MethodInsnNode mInsn, FlowFrame frame, FlowFrame nextFrame) {
    super(frame);
    boolean[] toInstrument = requireNonNull(argsToInstrument(mInsn.name, mInsn.desc));
    this.args = new FlowValue[toInstrument.length];
    for (int i = 0; i < args.length; i++) {
      if (toInstrument[i]) {
        args[i] = getStackFromTop(args.length - i - 1);
      }
    }
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
    if (args[0].isTrackable()) {
      args[0].ensureTracked();

      // we add these instructions using insertInvocationPreparation, so Invocation is on top of the
      // stack. setArg returns the Invocation, so it's on top of the stack again.
      InsnList toInsert = new InsnList();
      methodNode.addComment(toInsert, "begin InvocationArgStore.insertTrackStatements");
      toInsert.add(new InsnNode(Opcodes.ICONST_0)); // TODO make it not always 0
      args[0].loadSourcePoint(toInsert);
      toInsert.add(
          new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
              "be/coekaerts/wouter/flowtracker/tracker/Invocation",
              "setArg",
              "(ILbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;)"
                  + "Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;"));
      methodNode.addComment(toInsert, "end InvocationArgStore.insertTrackStatements");

      transformation.ensureInstrumented();
      transformation.insertInvocationPreparation(toInsert);

      methodNode.maxStack = Math.max(frame.getStackSize() + 3, methodNode.maxStack);
    }
  }

  static boolean shouldInstrumentInvocationArg(String name, String desc) {
    return argsToInstrument(name, desc) != null;
  }

  /** Determines which arguments should be instrumented. null if none of them should be. */
  static boolean[] argsToInstrument(String name, String desc) {
    Type[] args = Type.getArgumentTypes(desc);
    if (args.length != 1) {
      return null;
    }

    boolean[] result = new boolean[args.length];
    boolean any = false;

    boolean eager = name.contains("write") || name.contains("Write") || name.contains("print");

    for (int i = 0; i < args.length; i++) {
      Type arg = args[i];
      // instrument all char and byte args, because they're very likely to be relevant,
      // but for int arguments only if we're eager to because the name of the method suggests it
      // might be relevant.
      if (arg.getSort() == Type.CHAR || arg.getSort() == Type.BYTE
          || (eager && arg.getSort() == Type.INT)) {
        result[i] = true;
        any = true;
      }
    }
    return any ? result : null;
  }
}
