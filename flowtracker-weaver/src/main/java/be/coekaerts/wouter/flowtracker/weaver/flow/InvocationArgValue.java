package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

/** Value passed as an argument in a method that we track using {@link Invocation} */
class InvocationArgValue extends TrackableValue {
  InvocationArgValue(FlowMethodAdapter flowMethodAdapter, AbstractInsnNode insn) {
    super(flowMethodAdapter, Type.getArgumentTypes(flowMethodAdapter.desc)[0], insn);
  }

  @Override void insertTrackStatements() {
    flowMethodAdapter.invocation.ensureStarted(flowMethodAdapter);
  }

  @Override void loadSourceTracker(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert, "InvocationArgValue.loadSourceTracker");
    toInsert.add(flowMethodAdapter.invocation.invocationLocal.load());
    toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
        "be/coekaerts/wouter/flowtracker/tracker/Invocation",
        "getArg0Tracker",
        "(Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;)Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;"));
  }

  @Override void loadSourceIndex(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert, "InvocationArgValue.loadSourceIndex");
    toInsert.add(flowMethodAdapter.invocation.invocationLocal.load());
    toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
        "be/coekaerts/wouter/flowtracker/tracker/Invocation",
        "getArg0Index",
        "(Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;)I"));
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert, "InvocationArgValue.loadSourcePoint");
    toInsert.add(flowMethodAdapter.invocation.invocationLocal.load());
    toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
        "be/coekaerts/wouter/flowtracker/tracker/Invocation",
        "getArg0Point",
        "(Lbe/coekaerts/wouter/flowtracker/tracker/Invocation;)"
            + "Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;"));
  }
}
