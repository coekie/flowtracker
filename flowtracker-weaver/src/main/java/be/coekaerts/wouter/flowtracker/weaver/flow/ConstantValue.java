package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

public class ConstantValue extends TrackableValue {
  private final int value;
  private int offset = -1;

  ConstantValue(FlowMethodAdapter flowMethodAdapter, Type type,
      AbstractInsnNode insn, int value) {
    super(flowMethodAdapter, type, insn);
    this.value = value;
  }

  @Override
  void insertTrackStatements() {
    offset = flowMethodAdapter.constantsTransformation.trackConstant(
        flowMethodAdapter.name,
        value);
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    int classId = flowMethodAdapter.constantsTransformation.classId();

    if (flowMethodAdapter.canUseConstantDynamic()) {
      flowMethodAdapter.addComment(toInsert,
          "ConstantValue.loadSourcePoint: condy ConstantHook.constantPoint(%s, %s)", classId,
          offset);
      ConstantDynamic cd = new ConstantDynamic("$ft" + offset,
          "Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;",
          new Handle(Opcodes.H_INVOKESTATIC,
              "be/coekaerts/wouter/flowtracker/hook/ConstantHook",
              "constantPoint",
              "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;II)"
                  + "Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;",
              false),
          classId, offset);
      toInsert.add(new LdcInsnNode(cd));
    } else {
      flowMethodAdapter.addComment(toInsert,
          "ConstantValue.loadSourcePoint: ConstantHook.constantPoint(%s, %s)", classId, offset);
      toInsert.add(iconst(classId));
      toInsert.add(iconst(offset));
      toInsert.add(
          new MethodInsnNode(Opcodes.INVOKESTATIC,
              "be/coekaerts/wouter/flowtracker/hook/ConstantHook",
              "constantPoint",
              "(II)Lbe/coekaerts/wouter/flowtracker/tracker/TrackerPoint;"));
    }
  }

  // based on asm InstructionAdapter.iconst
  private static AbstractInsnNode iconst(int intValue) {
    if (intValue >= -1 && intValue <= 5) {
      return new InsnNode(Opcodes.ICONST_0 + intValue);
    } else if (intValue >= Byte.MIN_VALUE && intValue <= Byte.MAX_VALUE) {
      return new IntInsnNode(Opcodes.BIPUSH, intValue);
    } else if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
      return new IntInsnNode(Opcodes.SIPUSH, intValue);
    } else {
      return new LdcInsnNode(intValue);
    }
  }
}
