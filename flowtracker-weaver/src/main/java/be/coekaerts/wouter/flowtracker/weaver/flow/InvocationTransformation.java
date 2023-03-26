package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** Manages transformation of a called method that may return a tracked value */
class InvocationTransformation {
  /** Local variable that represents the Invocation of the method we're in */
  TrackLocal invocationLocal;

  /** Ensure that we called {@link Invocation#start(String)} at the beginning */
  void ensureStarted(FlowMethodAdapter methodNode) {
    if (invocationLocal != null) {
      return;
    }
    invocationLocal = methodNode.newLocal(
        Type.getType("Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;"),
        List.of(
            new LdcInsnNode(Invocation.signature(methodNode.name, methodNode.desc)),
            new MethodInsnNode(Opcodes.INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/tracker/Invocation",
            "start",
            "(Ljava/lang/String;)Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;",
            false)
        ),
        "InvocationTransformation invocation");
  }
}
