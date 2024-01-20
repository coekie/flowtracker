package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

/** Value returned from a called method, that we track using {@link Invocation} */
class InvocationReturnValue extends TrackableValue {
  final InvocationOutgoingTransformation transformation;

  /** Local variable storing the Invocation */
  private TrackLocal invocationLocal;

  InvocationReturnValue(FlowMethodAdapter flowMethodAdapter, MethodInsnNode mInsn) {
    super(flowMethodAdapter, Type.getReturnType(mInsn.desc), mInsn);
    this.transformation = new InvocationOutgoingTransformation(mInsn, flowMethodAdapter);
  }

  @Override void insertTrackStatements() {
    invocationLocal = flowMethodAdapter.newLocalForObject(
        Type.getType("Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;"),
        "InvocationReturnValue invocation");

    transformation.ensureInstrumented();
    transformation.storeInvocation(invocationLocal);
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert, "InvocationReturnValue.loadSourcePoint");
    toInsert.add(invocationLocal.load());
    toInsert.add(new FieldInsnNode(Opcodes.GETFIELD,
        "be/coekaerts/wouter/flowtracker/tracker/Invocation",
        "returnPoint",
        "Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;"));
  }

  static boolean shouldInstrumentInvocation(String name, String desc) {
    Type returnType = Type.getReturnType(desc);
    if (returnType.equals(Type.BYTE_TYPE) || returnType.equals(Type.CHAR_TYPE)) {
      return true;
    }

    // heuristic guessing which methods are worth tracking the return value of, because that
    // probably is a char or byte read from somewhere
    return returnType.equals(Type.INT_TYPE) && (name.contains("read") || name.contains("Read"))
        // don't instrument methods where the output is going through a buffer passed into it as
        // parameter. for those, the returned value is probably the length
        && !desc.contains("[") && !desc.contains("Buffer");
  }
}
