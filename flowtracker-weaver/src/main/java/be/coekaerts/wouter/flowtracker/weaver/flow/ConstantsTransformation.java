package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.ClassOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.ClassOriginTracker.ClassConstant;
import be.coekaerts.wouter.flowtracker.weaver.ClassFilter;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

/** Manages transformation for constant values ({@link ConstantValue}, and String constants) */
class ConstantsTransformation {
  private final String className;
  private final ClassFilter breakStringInterningFilter;
  private ClassOriginTracker tracker;
  private FlowMethodAdapter lastMethod;

  ConstantsTransformation(String className, ClassFilter breakStringInterningFilter) {
    this.className = className;
    this.breakStringInterningFilter = breakStringInterningFilter;
  }

  /**
   * Lazily initializes the {@link ClassOriginTracker}, so that only classes that actually have
   * something to track get registered.
   */
  private ClassOriginTracker tracker() {
    if (tracker == null) {
      tracker = ClassOriginTracker.registerClass(className);
    }
    return tracker;
  }

  /** Adds a header indicating a new method starts, if this is a new method */
  private void maybeAddMethodHeader(FlowMethodAdapter methodNode) {
    if (lastMethod != methodNode) {
      tracker().startMethod(methodDescription(methodNode));
      lastMethod = methodNode;
    }
  }

  ClassConstant trackConstant(FlowMethodAdapter methodNode, int value) {
    maybeAddMethodHeader(methodNode);
    return tracker().registerConstant(value);
  }

  int trackConstantString(FlowMethodAdapter methodNode, String value) {
    maybeAddMethodHeader(methodNode);
    return tracker().registerConstantString(value);
  }

  /**
   * ConstantDynamic representing the given String value, which should be at `offset` in `tracker`
   * (the offset returned by {@link #trackConstantString(FlowMethodAdapter, String)}).
   */
  ConstantDynamic stringConstantDynamic(int offset, String value) {
    return new ConstantDynamic("$ft" + offset,
        "Ljava/lang/String;",
        new Handle(Opcodes.H_INVOKESTATIC,
            "be/coekaerts/wouter/flowtracker/hook/StringHook",
            "constantString",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;IILjava/lang/String;)"
                + "Ljava/lang/String;",
            false),
        classId(), offset, value);
  }

  int classId() {
    return tracker().classId;
  }

  boolean canBreakStringInterning(FlowMethodAdapter methodNode) {
    return breakStringInterningFilter.include(methodNode.owner);
  }

  /** Human-friendly String representation of a method signature */
  private static String methodDescription(FlowMethodAdapter methodNode) {
    Type methodType = Type.getMethodType(methodNode.desc);
    StringBuilder sb = new StringBuilder();
    sb.append(methodType.getReturnType().getClassName());
    sb.append(' ');
    sb.append(methodNode.name);
    sb.append('(');
    boolean first = true;
    for (Type arg : methodType.getArgumentTypes()) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      sb.append(arg.getClassName());
    }
    sb.append(')');
    return sb.toString();
  }

  /** Create an InsnNode that loads a constant int value. Based on asm InstructionAdapter.iconst */
  static AbstractInsnNode iconst(int intValue) {
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
