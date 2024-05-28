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

import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethodAdapter;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Handles instrumentation of an LDC instruction that loads a String.
 */
class StringLdc extends Instrumentable {
  private final LdcInsnNode insn;
  private final FlowFrame frame;

  private StringLdc(LdcInsnNode insn, FlowFrame frame) {
    this.insn = insn;
    this.frame = frame;
  }

  @Override
  void instrument(FlowMethodAdapter methodNode) {
    ConstantsTransformation constantsTransformation = methodNode.constantsTransformation;
    if (constantsTransformation.canBreakStringInterning()) {
      String value = (String) insn.cst;
      int offset = constantsTransformation.trackConstantString(methodNode, value);
      // we prefer to use constant-dynamic, that is replacing just the LDC value. That is better for
      // performance, and at least partially maintains the properties of String interning: the
      // literal String loaded from the same location in the code will still always be the same
      // instance.
      // When necessary we fall back to replacing the LDC with a call to ConstantHook. (If we wanted
      // to then we could make a ConstantHook.constantPoint variant that still always return the
      // same instance, but we just haven't implemented that.)
      if (methodNode.canUseConstantDynamic()) {
        insn.cst = constantsTransformation.stringConstantDynamic(offset, value);
      } else {
        InsnList toInsert = new InsnList();
        methodNode.addComment(toInsert, "begin StringLdc.insertTrackStatements");
        toInsert.add(ConstantsTransformation.iconst(constantsTransformation.classId()));
        toInsert.add(ConstantsTransformation.iconst(offset));
        toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
            "com/coekie/flowtracker/hook/StringHook", "constantString",
            "(Ljava/lang/String;II)Ljava/lang/String;"));
        methodNode.addComment(toInsert, "end StringLdc.insertTrackStatements");
        methodNode.instructions.insert(insn, toInsert);
        methodNode.maxStack = Math.max(frame.fullStackSize() + 3, methodNode.maxStack);
      }
    }
  }

  /** Add a {@link StringLdc} to `toInstrument` when we need to instrument it */
  static void analyze(List<Instrumentable> toInstrument, LdcInsnNode insn, FlowFrame frame) {
    if (insn.cst instanceof String) {
      toInstrument.add(new StringLdc(insn, frame));
    }
  }
}
