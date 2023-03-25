package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class ConstantValue extends TrackableValue {
  private final String descriptor;

  ConstantValue(FlowMethodAdapter flowMethodAdapter, Type type, String descriptor) {
    super(flowMethodAdapter, type);
    this.descriptor = descriptor;
  }

  @Override
  void insertTrackStatements() {
    // nothing to do
  }

  @Override
  void loadSourceTracker(InsnList toInsert) {
    // NICE the Tracker could be a ConstantDynamic
    flowMethodAdapter.addComment(toInsert,
        "ConstantValue.loadSourceTracker: ConstantHook.constantTracker(\"%s\")", descriptor);
    toInsert.add(new LdcInsnNode(descriptor));
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/hook/ConstantHook",
            "constantTracker",
            "(Ljava/lang/String;)Lbe/coekaerts/wouter/flowtracker/tracker/Tracker;",
            false));
  }

  @Override
  void loadSourceIndex(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert, "ConstantValue.loadSourceIndex: 0");
    toInsert.add(new InsnNode(Opcodes.ICONST_0));
  }
}
