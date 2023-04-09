package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** Value returned from a called method, that we track using {@link Invocation} */
class InvocationReturnValue extends TrackableValue {
  /** The call */
  private final MethodInsnNode mInsn;

  /** Local variable storing the Invocation */
  private TrackLocal invocationLocal;

  InvocationReturnValue(FlowMethodAdapter flowMethodAdapter, MethodInsnNode mInsn) {
    super(flowMethodAdapter, Type.getReturnType(mInsn.desc), mInsn);
    this.mInsn = mInsn;
  }

  @Override void insertTrackStatements() {
    // on the stack before the charAt call: String target, int index

    invocationLocal = flowMethodAdapter.newLocalForObject(
        Type.getType("Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;"),
        "InvocationReturnValue invocation");

    InsnList toInsert = new InsnList();

    flowMethodAdapter.addComment(toInsert, "InvocationReturnValue.insertTrackStatements");
    toInsert.add(new LdcInsnNode(Invocation.signature(mInsn.name, mInsn.desc)));
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/tracker/Invocation",
            "calling",
            "(Ljava/lang/String;)Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;",
            false));
    toInsert.add(invocationLocal.store());

    flowMethodAdapter.instructions.insertBefore(mInsn, toInsert);
  }

  @Override void loadSourceTracker(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert, "InvocationReturnValue.loadSourceTracker");
    toInsert.add(invocationLocal.load());
    toInsert.add(new FieldInsnNode(Opcodes.GETFIELD,
        "be/coekaerts/wouter/flowtracker/tracker/Invocation",
        "returnTracker",
        "Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;"));
  }

  @Override void loadSourceIndex(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert, "InvocationReturnValue.loadSourceIndex");
    toInsert.add(invocationLocal.load());
    toInsert.add(new FieldInsnNode(Opcodes.GETFIELD,
        "be/coekaerts/wouter/flowtracker/tracker/Invocation",
        "returnIndex",
        "I"));
  }

  // TODO better conditions for when to track call
  static boolean shouldInstrumentInvocation(String name, String desc) {
    // heuristic guessing which methods are worth tracking the return value of, because that
    // probably is a char or byte read from somewhere
    return name.startsWith("read") && desc.endsWith("I")
        // don't instrument methods where the output is going through a buffer passed into it as
        // parameter. for those, the returned value is probably the length
        && !desc.contains("[") && !desc.contains("Buffer");
  }
}
