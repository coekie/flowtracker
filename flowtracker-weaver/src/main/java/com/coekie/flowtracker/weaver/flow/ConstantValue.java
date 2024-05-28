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

import com.coekie.flowtracker.tracker.ClassOriginTracker.ClassConstant;
import com.coekie.flowtracker.util.RecursionChecker;
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

class ConstantValue extends TrackableValue {
  private final int value;
  private ClassConstant constant;

  ConstantValue(FlowMethod method, Type type,
      AbstractInsnNode insn, int value) {
    super(method, type, insn);
    this.value = value;
  }

  @Override
  void insertTrackStatements() {
    constant = method.constantsTransformation.trackConstant(method, value);
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    // we prefer to use constant-dynamic, for performance, but fall back to invoking
    // ConstantHook.constantPoint every time when necessary.
    if (method.canUseConstantDynamic()) {
      method.addComment(toInsert,
          "ConstantValue.loadSourcePoint: condy ConstantHook.constantPoint(%s, %s, %s)",
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
    } else {
      method.addComment(toInsert,
          "ConstantValue.loadSourcePoint: ConstantHook.constantPoint(%s, %s)",
          constant.classId, constant.offset);
      toInsert.add(ConstantsTransformation.iconst(constant.classId));
      toInsert.add(ConstantsTransformation.iconst(constant.offset));
      toInsert.add(
          new MethodInsnNode(Opcodes.INVOKESTATIC,
              "com/coekie/flowtracker/hook/ConstantHook",
              "constantPoint",
              "(II)Lcom/coekie/flowtracker/tracker/TrackerPoint;"));
      if (constant.length != 1) {
        method.addComment(toInsert,
            "ConstantValue.loadSourcePoint: ConstantHook.withLength(%s)", constant.length);
        toInsert.add(ConstantsTransformation.iconst(constant.length));
        toInsert.add(
            new MethodInsnNode(Opcodes.INVOKESTATIC,
                "com/coekie/flowtracker/hook/ConstantHook",
                "withLength",
                "(Lcom/coekie/flowtracker/tracker/TrackerPoint;I)Lcom/coekie/flowtracker/tracker/TrackerPoint;"));
      }
    }
  }
}
