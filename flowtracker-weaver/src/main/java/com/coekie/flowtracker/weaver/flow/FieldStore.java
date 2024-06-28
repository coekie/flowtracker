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
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** The storing of a value in a field. */
// on the stack: Object target, [int|char|byte] value
class FieldStore extends Store {
  private final FieldInsnNode storeInsn;
  private final FlowValue storedValue = getStackFromTop(0);

  private FieldStore(FieldInsnNode storeInsn, FlowFrame frame) {
    super(frame);
    this.storeInsn = storeInsn;
  }

  void instrument(FlowMethod methodNode) {
    // only track char or byte
    if (!FieldValue.shouldTrack(storedValue.getType(), storeInsn)) {
      return;
    }

    InsnList toInsert = new InsnList();

    methodNode.addComment(toInsert, "begin FieldStore.instrument");
    // put a copy on top of the stack to pass as argument into the hook method, in two steps:
    // starting with (target, value) on the stack,
    // dup2 -> (target, value, target, value),
    toInsert.add(new InsnNode(Opcodes.DUP2));
    // pop -> (target, value, target).
    toInsert.add(new InsnNode(Opcodes.POP));
    toInsert.add(new LdcInsnNode(FieldRepository.fieldId(storeInsn.owner, storeInsn.name)));

    // note: we do this even for UntrackableValues
    storedValue.ensureTracked();
    loadSourcePointOrFallback(storedValue, toInsert);

    methodNode.maxStack = Math.max(frame.fullStackSize() + 5, methodNode.maxStack);

    toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
        "com/coekie/flowtracker/tracker/FieldRepository", "setPoint",
        "(Ljava/lang/Object;Ljava/lang/String;Lcom/coekie/flowtracker/tracker/TrackerPoint;)V",
        false));

    methodNode.addComment(toInsert, "end FieldStore.instrument");

    methodNode.instructions.insertBefore(storeInsn, toInsert);
  }

  /** Add a {@link FieldStore} to `toInstrument` when we need to instrument it */
  static void analyze(List<Instrumentable> toInstrument, FieldInsnNode insn, FlowFrame frame) {
    toInstrument.add(new FieldStore(insn, frame));
  }
}
