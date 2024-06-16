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

/**
 * A value from calling {@link String#charAt(int)}.
 * This could have been handled by {@link InvocationReturnValue}, but we handle this differently as
 * an optimization, because String.charAt can be called often and Invocation instrumentation is
 * slower.
 */
// on the stack: String target, int index
class CharAtValue extends TrackableValue {
  /** The charAt call */
  private final MethodInsnNode insn;
  private final boolean onCharSequence;

  /** Local variable storing the PointTracker for the loaded char */
  private TrackLocal pointTrackerLocal;

  CharAtValue(FlowMethod method, MethodInsnNode insn, boolean onCharSequence) {
    super(method, Type.CHAR_TYPE, insn);
    this.insn = insn;
    this.onCharSequence = onCharSequence;
  }

  @Override void insertTrackStatements() {
    // on the stack before the call: String target, int index

    pointTrackerLocal = method.newLocalForObject(
        Type.getType("Lcom/coekie/flowtracker/tracker/TrackerPoint;"),
        "CharAtValue PointTracker");

    InsnList toInsert = new InsnList();
    method.addComment(toInsert, "begin CharAtValue.insertTrackStatements");

    // insert code for: pointTracker = StringHook.chatAtTracker(target, index, context)
    // use DUP2 to copy target and index for chatAtTracker while leaving it on the stack for the
    // actual chatAt call
    toInsert.add(new InsnNode(Opcodes.DUP2));
    toInsert.add(method.loadContext());
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKESTATIC,
            "com/coekie/flowtracker/hook/StringHook",
            "charAtTracker",
            onCharSequence
            ? "(Ljava/lang/CharSequence;ILcom/coekie/flowtracker/tracker/Context;)"
                + "Lcom/coekie/flowtracker/tracker/TrackerPoint;"
                : "(Ljava/lang/String;ILcom/coekie/flowtracker/tracker/Context;)"
                + "Lcom/coekie/flowtracker/tracker/TrackerPoint;",
            false));
    toInsert.add(pointTrackerLocal.store());
    method.maxStack = Math.max(method.maxStack,
        getCreationFrame().fullStackSize() + 3);

    method.addComment(toInsert, "end CharAtValue.insertTrackStatements");

    method.instructions.insertBefore(insn, toInsert);
  }

  @Override
  void loadSourcePoint(InsnList toInsert, FallbackSource fallback) {
    method.addComment(toInsert, "CharAtValue.loadSourcePoint");
    toInsert.add(pointTrackerLocal.load());
  }
}
