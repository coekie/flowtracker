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

import com.coekie.flowtracker.tracker.FieldRepository;
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** A value from getting a field value ({@link Opcodes#GETFIELD}) */
class FieldValue extends TrackableValue {
  /** The {@link Opcodes#GETFIELD} call */
  private final FieldInsnNode insn;

  /** Local variable storing the PointTracker for the loaded char or byte */
  private TrackLocal pointTrackerLocal;

  FieldValue(FlowMethodAdapter flowMethodAdapter, FieldInsnNode insn, Type type) {
    super(flowMethodAdapter, type, insn);
    this.insn = insn;
  }

  @Override void insertTrackStatements() {
    // on the stack before the GETFIELD call: target

    pointTrackerLocal = flowMethodAdapter.newLocalForObject(
        Type.getType("Lcom/coekie/flowtracker/tracker/TrackerPoint;"),
        "FieldValue PointTracker");

    InsnList toInsert = new InsnList();
    flowMethodAdapter.addComment(toInsert, "begin FieldValue.insertTrackStatements");

    // insert code for: pointTracker = FieldRepository.getPoint(target, fieldId)
    // use DUP to copy target for getPoint while leaving it on the stack for the actual GETFIELD
    toInsert.add(new InsnNode(Opcodes.DUP));
    toInsert.add(new LdcInsnNode(FieldRepository.fieldId(insn.owner, insn.name)));
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC,
            "com/coekie/flowtracker/tracker/FieldRepository",
            "getPoint",
            "(Ljava/lang/Object;Ljava/lang/String;)"
                + "Lcom/coekie/flowtracker/tracker/TrackerPoint;",
            false));
    toInsert.add(pointTrackerLocal.store());

    flowMethodAdapter.maxStack = Math.max(flowMethodAdapter.maxStack,
        getCreationFrame().fullStackSize() + 2);

    flowMethodAdapter.addComment(toInsert, "end FieldValue.insertTrackStatements");

    flowMethodAdapter.instructions.insertBefore(insn, toInsert);
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert, "FieldValue.loadSourcePoint");
    toInsert.add(pointTrackerLocal.load());
  }

  static boolean shouldTrack(Type type, FieldInsnNode node) {
    if (!shouldTrackType(type)) {
      return false;
    }
    switch (node.owner) {
      // optimization: don't track String.coder
      case "java/lang/String":
      case "java/lang/AbstractStringBuilder":
        if (node.name.equals("coder")) {
          return false;
        }
        break;
    }

    return true;
  }

  static boolean shouldTrackType(Type type) {
    if (type == null) {
      return false;
    }
    int sort = type.getSort();
    return sort == Type.CHAR || sort == Type.BYTE;
  }
}
