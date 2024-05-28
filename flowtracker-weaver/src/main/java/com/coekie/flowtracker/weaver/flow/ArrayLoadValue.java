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

import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** A value from getting an element of an array (e.g. {@link Opcodes#CALOAD}) */
// on the stack: char[]/byte[]/int[] target, int index
class ArrayLoadValue extends TrackableValue {
  /** The CALOAD/BALOAD/IALOAD call */
  private final InsnNode insn;

  /** Local variable storing the PointTracker for the loaded char or byte */
  private TrackLocal pointTrackerLocal;

  ArrayLoadValue(FlowMethod method, InsnNode insn, Type type) {
    super(method, type, insn);
    this.insn = insn;
  }

  @Override void insertTrackStatements() {
    // on the stack before the CALOAD call: char[] target, int index

    pointTrackerLocal = method.newLocalForObject(
        Type.getType("Lcom/coekie/flowtracker/tracker/TrackerPoint;"),
        "ArrayLoadValue PointTracker");

    InsnList toInsert = new InsnList();
    method.addComment(toInsert, "begin ArrayLoadValue.insertTrackStatements");

    // insert code for: pointTracker = ArrayLoadHook.getElementTracker(target, index)
    // use DUP2 to copy target and index for getElementTracker while leaving it on the stack for the
    // actual CALOAD
    toInsert.add(new InsnNode(Opcodes.DUP2));
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC,
            "com/coekie/flowtracker/hook/ArrayLoadHook",
            "getElementTracker",
            "(Ljava/lang/Object;I)Lcom/coekie/flowtracker/tracker/TrackerPoint;",
            false));
    toInsert.add(pointTrackerLocal.store());
    method.maxStack = Math.max(method.maxStack,
        getCreationFrame().fullStackSize() + 2);

    method.addComment(toInsert, "end ArrayLoadValue.insertTrackStatements");

    method.instructions.insertBefore(insn, toInsert);
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    method.addComment(toInsert, "ArrayLoadValue.loadSourcePoint");
    toInsert.add(pointTrackerLocal.load());
  }
}
