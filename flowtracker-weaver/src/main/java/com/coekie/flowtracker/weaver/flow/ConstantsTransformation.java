package com.coekie.flowtracker.weaver.flow;

/*-
 * Copyright 2024 Wouter Coekaerts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.tracker.ClassOriginTracker.ClassConstant;
import com.coekie.flowtracker.util.RecursionChecker;
import com.coekie.flowtracker.weaver.ClassFilter;
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
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

/** Manages transformation for constant values ({@link ConstantValue}, and String constants) */
class ConstantsTransformation {
  private final String className;
  private final ClassFilter breakStringInterningFilter;
  private ClassOriginTracker tracker;
  private FlowMethod lastMethod;

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
  private void maybeAddMethodHeader(FlowMethod methodNode) {
    if (lastMethod != methodNode) {
      tracker().startMethod(methodDescription(methodNode));
      lastMethod = methodNode;
    }
  }

  ClassConstant trackConstant(FlowMethod methodNode, int value, int line) {
    maybeAddMethodHeader(methodNode);
    return tracker().registerConstant(value, line);
  }

  ClassConstant fallback(FlowMethod methodNode, int line) {
    maybeAddMethodHeader(methodNode);
    return tracker().registerFallback(line);
  }

  int trackConstantString(FlowMethod methodNode, String value, int line) {
    maybeAddMethodHeader(methodNode);
    return tracker().registerConstantString(value, line);
  }

  /**
   * ConstantDynamic representing the given String value, which should be at `offset` in `tracker`
   * (the offset returned by {@link #trackConstantString(FlowMethod, String, int)}).
   */
  ConstantDynamic stringConstantDynamic(int offset, String value) {
    return new ConstantDynamic("$ft" + offset,
        "Ljava/lang/String;",
        new Handle(Opcodes.H_INVOKESTATIC,
            "com/coekie/flowtracker/hook/StringHook",
            "constantString",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;IILjava/lang/String;)"
                + "Ljava/lang/String;",
            false),
        classId(), offset, value);
  }

  int classId() {
    return tracker().classId;
  }

  boolean canBreakStringInterning() {
    return breakStringInterningFilter.include(className);
  }

  /** Human-friendly String representation of a method signature */
  private static String methodDescription(FlowMethod methodNode) {
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

  static void loadClassConstantPoint(InsnList toInsert, FlowMethod method, ClassConstant constant) {
    // we prefer to use constant-dynamic, for performance, but fall back to invoking
    // ConstantHook.constantPoint every time when necessary.
    if (method.canUseConstantDynamic()) {
      loadClassConstantPointWithCondy(toInsert, method, constant);
    } else {
      loadClassConstantPointWithoutCondy(toInsert, method, constant);
    }
  }

  private static void loadClassConstantPointWithCondy(InsnList toInsert, FlowMethod method,
      ClassConstant constant) {
    method.addComment(toInsert,
        "loadClassConstantPoint: condy ConstantHook.constantPoint(%s, %s, %s)",
        constant.classId, constant.offset, constant.length);
    if (RecursionChecker.enabled()) {
      toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "com/coekie/flowtracker/util/RecursionChecker", "before", "()V"));
    }
    ConstantDynamic cd = new ConstantDynamic("$ft" + constant.offset,
        "Lcom/coekie/flowtracker/tracker/TrackerPoint;",
        new Handle(Opcodes.H_INVOKESTATIC,
            "com/coekie/flowtracker/hook/ConstantHook",
            "constantPoint",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;III)"
                + "Lcom/coekie/flowtracker/tracker/TrackerPoint;",
            false),
        constant.classId, constant.offset, constant.length);
    toInsert.add(new LdcInsnNode(cd));
    if (RecursionChecker.enabled()) {
      toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "com/coekie/flowtracker/util/RecursionChecker", "after", "()V"));
    }
  }

  private static void loadClassConstantPointWithoutCondy(InsnList toInsert, FlowMethod method,
      ClassConstant constant) {
    method.addComment(toInsert,
        "loadClassConstantPoint: ConstantHook.constantPoint(%s, %s)",
        constant.classId, constant.offset);
    toInsert.add(iconst(constant.classId));
    toInsert.add(iconst(constant.offset));
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC,
            "com/coekie/flowtracker/hook/ConstantHook",
            "constantPoint",
            "(II)Lcom/coekie/flowtracker/tracker/TrackerPoint;"));
    if (constant.length != 1) {
      method.addComment(toInsert,
          "ConstantValue.loadSourcePoint: ConstantHook.withLength(%s)", constant.length);
      toInsert.add(iconst(constant.length));
      toInsert.add(
          new MethodInsnNode(Opcodes.INVOKESTATIC,
              "com/coekie/flowtracker/hook/ConstantHook",
              "withLength",
              "(Lcom/coekie/flowtracker/tracker/TrackerPoint;I)Lcom/coekie/flowtracker/tracker/TrackerPoint;"));
    }
  }
}
