package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.ClassOriginTracker.ClassConstant;
import be.coekaerts.wouter.flowtracker.util.RecursionChecker;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class ConstantValue extends TrackableValue {
  private final int value;
  private ClassConstant constant;

  ConstantValue(FlowMethodAdapter flowMethodAdapter, Type type,
      AbstractInsnNode insn, int value) {
    super(flowMethodAdapter, type, insn);
    this.value = value;
  }

  @Override
  void insertTrackStatements() {
    constant = flowMethodAdapter.constantsTransformation.trackConstant(flowMethodAdapter, value);
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    // we prefer to use constant-dynamic, for performance, but fall back to invoking
    // ConstantHook.constantPoint every time when necessary.
    if (flowMethodAdapter.canUseConstantDynamic()) {
      flowMethodAdapter.addComment(toInsert,
          "ConstantValue.loadSourcePoint: condy ConstantHook.constantPoint(%s, %s, %s)",
          constant.classId, constant.offset, constant.length);
      if (RecursionChecker.enabled()) {
        toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/util/RecursionChecker", "before", "()V"));
      }
      ConstantDynamic cd = new ConstantDynamic("$ft" + constant.offset,
          "Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;",
          new Handle(Opcodes.H_INVOKESTATIC,
              "be/coekaerts/wouter/flowtracker/hook/ConstantHook",
              "constantPoint",
              "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;III)"
                  + "Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;",
              false),
          constant.classId, constant.offset, constant.length);
      toInsert.add(new LdcInsnNode(cd));
      if (RecursionChecker.enabled()) {
        toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/util/RecursionChecker", "after", "()V"));
      }
    } else {
      flowMethodAdapter.addComment(toInsert,
          "ConstantValue.loadSourcePoint: ConstantHook.constantPoint(%s, %s)",
          constant.classId, constant.offset);
      toInsert.add(ConstantsTransformation.iconst(constant.classId));
      toInsert.add(ConstantsTransformation.iconst(constant.offset));
      toInsert.add(
          new MethodInsnNode(Opcodes.INVOKESTATIC,
              "be/coekaerts/wouter/flowtracker/hook/ConstantHook",
              "constantPoint",
              "(II)Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;"));
      if (constant.length != 1) {
        flowMethodAdapter.addComment(toInsert,
            "ConstantValue.loadSourcePoint: ConstantHook.withLength(%s)", constant.length);
        toInsert.add(ConstantsTransformation.iconst(constant.length));
        toInsert.add(
            new MethodInsnNode(Opcodes.INVOKESTATIC,
                "be/coekaerts/wouter/flowtracker/hook/ConstantHook",
                "withLength",
                "(Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;I)Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;"));
      }
    }
  }
}
