package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * The passing of a value as argument into the invocation of another method that may be instrumented
 * with {@link Invocation}.
 */
public class InvocationArgStore extends Store {
  /** The call */
  private final MethodInsnNode mInsn;
  // for now, we only support calls with one argument
  private final FlowValue arg0 = getStackFromTop(0);

  InvocationArgStore(MethodInsnNode mInsn, FlowFrame frame) {
    super(frame);
    this.mInsn = mInsn;
  }

  @Override
  void insertTrackStatements(FlowMethodAdapter methodNode) {
    // if we know where the value passed in as argument came from
    if (arg0.isTrackable()) {
      arg0.ensureTracked();

      InsnList toInsert = new InsnList();
      methodNode.addComment(toInsert, "begin InvocationArgStore.insertTrackStatements");
      toInsert.add(new LdcInsnNode(Invocation.signature(mInsn.name, mInsn.desc)));
      toInsert.add(
          new MethodInsnNode(Opcodes.INVOKESTATIC,
              "be/coekaerts/wouter/flowtracker/tracker/Invocation",
              "calling",
              "(Ljava/lang/String;)Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;"));
      arg0.loadSourceTracker(toInsert);
      arg0.loadSourceIndex(toInsert);
      toInsert.add(
          new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
              "be/coekaerts/wouter/flowtracker/tracker/Invocation",
              "setArg0",
              "(Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;I)"
                  + "Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;"));
      // don't need the Invocation anymore. (we would if we'd also want to track the return value,
      // in InvocationReturnValue. but for now, we do not support both InvocationArgStore and
      // InvocationReturnValue on the same invocation.
      toInsert.add(new InsnNode(Opcodes.POP));

      methodNode.addComment(toInsert, "end InvocationArgStore.insertTrackStatements");
      methodNode.maxStack = Math.max(frame.getStackSize() + 3, methodNode.maxStack);

      methodNode.instructions.insertBefore(mInsn, toInsert);
    }
  }

  // TODO better conditions for when to track call
  static boolean shouldInstrumentInvocationArg(String name, String desc) {
    if (!(name.startsWith("write") || name.startsWith("print"))) {
      return false;
    }
    Type[] args = Type.getArgumentTypes(desc);
    return args.length == 1
        && (args[0].equals(Type.CHAR_TYPE) || args[0].equals(Type.BYTE_TYPE)
        || args[0].equals(Type.INT_TYPE));
  }
}
