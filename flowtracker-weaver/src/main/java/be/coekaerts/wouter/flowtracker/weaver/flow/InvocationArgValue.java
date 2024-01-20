package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** Value passed as an argument in a method that we track using {@link Invocation} */
class InvocationArgValue extends TrackableValue {
  private final int argNum;
  InvocationArgValue(FlowMethodAdapter flowMethodAdapter, AbstractInsnNode insn, int argNum) {
    super(flowMethodAdapter, Type.getArgumentTypes(flowMethodAdapter.desc)[argNum], insn);
    if (argNum > InvocationArgStore.MAX_ARG_NUM_TO_INSTRUMENT + 1) {
      throw new IllegalArgumentException();
    }
    this.argNum = argNum;
  }

  @Override void insertTrackStatements() {
    flowMethodAdapter.invocation.ensureStarted(flowMethodAdapter);
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert, "InvocationArgValue.loadSourcePoint");
    toInsert.add(flowMethodAdapter.invocation.invocationLocal.load());
    toInsert.add(new InsnNode(Opcodes.ICONST_0 + argNum));
    toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
        "be/coekaerts/wouter/flowtracker/tracker/Invocation",
        "getArgPoint",
        "(Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;I)"
            + "Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;"));
  }
}
