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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;

/** Extension of ASM's {@link Analyzer}, used for flow analysis */
class FlowAnalyzer extends Analyzer<FlowValue> {
  final FlowMethod method;

  FlowAnalyzer(FlowMethod method) {
    super(new FlowInterpreter(method));
    this.method = method;
  }

  @Override
  protected Frame<FlowValue> newFrame(int numLocals, int numStack) {
    return new FlowFrame(numLocals, numStack, this);
  }

  @Override
  protected Frame<FlowValue> newFrame(Frame<? extends FlowValue> frame) {
    return new FlowFrame((FlowFrame) frame, this);
  }

  @Override
  public Frame<FlowValue>[] analyze(String owner, MethodNode method) throws AnalyzerException {
    Frame<FlowValue>[] frames = super.analyze(owner, method);

    for (int i = 0; i < frames.length; i++) {
      // make frames aware of which instruction they correspond to.
      // note: if we need this to be initialized earlier, we could also override
      // newControlFlowEdge & newControlFlowExceptionEdge and initialize it at that point
      FlowFrame frame = (FlowFrame) frames[i];
      if (frame != null) {
        frame.initInsn(i);
      }
    }

    return frames;
  }

  /**
   * Gets the FlowFrame that represents the state of local variables and stack at the given
   * instruction. This must be called after the analyzer ran, but before any changes have been
   * made.
   */
  FlowFrame getFrame(AbstractInsnNode insn) {
    int index = method.instructions.indexOf(insn);
    FlowFrame frame = (FlowFrame) getFrames()[index];
    if (frame.getInsn() != insn) {
      throw new IllegalStateException("Instruction and frame index don't match");
    }
    return frame;
  }

  @Override
  protected boolean newControlFlowExceptionEdge(int insnIndex, TryCatchBlockNode tryCatchBlock) {
    AbstractInsnNode insn = method.instructions.get(insnIndex);
    // optimization: these instructions can never throw, so do not analyze the code path from them
    // to an exception handler. this reduces the number of merges we have to do. our merge handling
    // is relatively slow. there are many more like that; but this already reduces the
    // false-positive merges a lot.
    if (insn instanceof LabelNode || insn instanceof FrameNode || insn instanceof VarInsnNode) {
      return false;
    }
    return super.newControlFlowExceptionEdge(insnIndex, tryCatchBlock);
  }
}
