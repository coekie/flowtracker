package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.tracker.ClassOriginTracker;
import be.coekaerts.wouter.flowtracker.weaver.ClassFilter;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

/** Manages transformation for constant values ({@link ConstantValue}, and String constants) */
class ConstantsTransformation {
  private final String className;
  private final ClassFilter breakStringInterningFilter;
  private ClassOriginTracker tracker;

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

  int trackConstant(String method, int value) {
    return tracker().registerConstant(method, value);
  }

  int trackConstantString(String method, String value) {
    return tracker().registerConstantString(method, value);
  }

  int classId() {
    return tracker().classId;
  }

  boolean canBreakStringInterning(FlowMethodAdapter methodNode) {
    return breakStringInterningFilter.include(methodNode.owner);
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
