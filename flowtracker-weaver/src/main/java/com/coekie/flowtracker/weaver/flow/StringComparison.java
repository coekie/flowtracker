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

import com.coekie.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Instruments a "==" or "!=" comparison where at least one of the operands is known to be String.
 * This partially undoes the bad effects of {@link StringLdc} breaking String interning.
 * This is far from perfect; it e.g. doesn't "fix" comparisons between two Object references that in
 * practice contain Strings, doesn't fix identityHashCode,....
 * And it also causes false positives" it makes all Strings with equal contents "==" each other.
 */
public class StringComparison extends Store {
  private final JumpInsnNode insn;
  private final boolean firstIsString;

  StringComparison(JumpInsnNode insn, FlowFrame frame, boolean firstIsString) {
    super(frame);
    this.insn = insn;
    this.firstIsString = firstIsString;
  }

  void insertTrackStatements(FlowMethodAdapter methodNode) {
    InsnList toInsert = new InsnList();
    methodNode.addComment(toInsert, "StringComparison.insertTrackStatements");

    if (!firstIsString) {
      // if we're not sure that the first object is String (only the second one), then swap them,
      // to avoid calling .equals on anything else than a String.
      toInsert.add(new InsnNode(Opcodes.SWAP));
    }
    toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
        "java/util/Objects",
        "equals",
        "(Ljava/lang/Object;Ljava/lang/Object;)Z",
        false));
    methodNode.instructions.insertBefore(insn, toInsert);

    if (insn.getOpcode() == Opcodes.IF_ACMPEQ) {
      insn.setOpcode(Opcodes.IFNE);
    } else if (insn.getOpcode() == Opcodes.IF_ACMPNE) {
      insn.setOpcode(Opcodes.IFEQ);
    } else {
      throw new IllegalStateException();
    }
  }
}
