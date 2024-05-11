package com.coekie.flowtracker.weaver.flow;

import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
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
        "com/coekie/flowtracker/tracker/Invocation",
        "getArgPoint",
        "(Lcom/coekie/flowtracker/tracker/Invocation;I)"
            + "Lcom/coekie/flowtracker/tracker/TrackerPoint;"));
  }
}
