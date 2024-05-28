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

import com.coekie.flowtracker.weaver.Types;
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** The storing of a value in an array, e.g. a char in a char[]. */
// on the stack: char[] target, int index, char toStore 
class ArrayStore extends Store {
  private final InsnNode storeInsn;
  /** Method in ArrayHook to call as a replacement for the array store operation */
  private final String hookMethod;
  private final FlowValue storedValue = getStackFromTop(0);

  private ArrayStore(InsnNode storeInsn, FlowFrame frame, String hookMethod) {
    super(frame);
    this.storeInsn = storeInsn;
    this.hookMethod = hookMethod;
  }

  void instrument(FlowMethod methodNode) {
    InsnList toInsert = new InsnList();

    methodNode.addComment(toInsert, "begin ArrayStore.insertTrackStatements: "
        + "ArrayHook.set*(array, arrayIndex, value [already on stack], sourcePoint)");

    // note: we do this even for UntrackableValues
    storedValue.ensureTracked();
    storedValue.loadSourcePoint(toInsert);

    methodNode.maxStack = Math.max(frame.fullStackSize() + 3, methodNode.maxStack);

    Method hook = Method.getMethod(hookMethod);

    toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
        "com/coekie/flowtracker/hook/ArrayHook", hook.getName(), hook.getDescriptor(),
        false));

    methodNode.addComment(toInsert, "end ArrayStore.insertTrackStatements");

    methodNode.instructions.insert(storeInsn, toInsert);
    methodNode.instructions.remove(storeInsn); // our hook takes care of the storing
  }

  /** Add a {@link ArrayStore} to `toInstrument` when we need to instrument it */
  static void analyzeCharArrayStore(List<Instrumentable> toInstrument, InsnNode insn,
      FlowFrame frame) {
    toInstrument.add(new ArrayStore(insn, frame,
        "void setChar(char[],int,char,com.coekie.flowtracker.tracker.TrackerPoint)"));
  }

  /** Add a {@link ArrayStore} to `toInstrument` when we need to instrument it */
  static void analyzeByteArrayStore(List<Instrumentable> toInstrument, InsnNode insn,
      FlowFrame frame) {
    if (Types.BYTE_ARRAY.equals(frame.getStack(frame.getStackSize() - 3).getType())) {
      toInstrument.add(new ArrayStore(insn, frame,
          "void setByte(byte[],int,byte,com.coekie.flowtracker.tracker.TrackerPoint)"));
    }
  }

  /** Add a {@link ArrayStore} to `toInstrument` when we need to instrument it */
  static void analyzeIntArrayStore(List<Instrumentable> toInstrument, InsnNode insn,
      FlowFrame frame, String owner) {
    // dirty heuristic for when we want to instrument array stores.
    // this is necessary because of some bootstrapping problem leaving to StackOverflowError
    // in tests, but only if they're being ran from maven, and not if you attach a debugger
    // (heisenbug).
    // ideally we'd only do this for arrays that deal with codepoints, which are very rare.
    if (!owner.startsWith("java/lang") && !owner.startsWith("java/util")) {
      toInstrument.add(new ArrayStore(insn, frame,
          "void setInt(int[],int,int,com.coekie.flowtracker.tracker.TrackerPoint)"));
    }
  }
}
