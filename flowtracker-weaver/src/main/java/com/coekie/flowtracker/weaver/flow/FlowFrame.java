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

import static java.util.Objects.requireNonNull;

import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Interpreter;

/** {@link Frame} used in flow analysis */
class FlowFrame extends Frame<FlowValue> {
  /** Analyzer that created this frame */
  final FlowAnalyzer analyzer;

  /** Instruction that this frame corresponds to */
  private AbstractInsnNode insn;

  private final Map<ValueReference, Set<FlowValue>> mergedValues = new HashMap<>();

  FlowFrame(int numLocals, int maxStack, FlowAnalyzer analyzer) {
    super(numLocals, maxStack);
    this.analyzer = analyzer;
  }

  FlowFrame(Frame<? extends FlowValue> frame, FlowAnalyzer analyzer) {
    super(frame);
    this.analyzer = analyzer;
  }

  AbstractInsnNode getInsn() {
    if (insn == null) {
      throw new IllegalStateException("FlowFrame.insn not initialized");
    }
    return insn;
  }

  void initInsn(int insnIndex) {
    if (analyzer.getFrames()[insnIndex] != this) {
      throw new IllegalStateException("Wrong instruction index");
    }
    insn = requireNonNull(analyzer.method.instructions.get(insnIndex));
  }

  FlowMethod getMethod() {
    return analyzer.method;
  }

  int getLine() {
    return getMethod().getLine(getInsn());
  }

  /** Stack size as should be reported in byte code, counting long and double as two */
  int fullStackSize() {
    int stackSize = 0;
    for (int i = 0; i < getStackSize(); ++i) {
      stackSize += getStack(i).getSize();
    }
    return stackSize;
  }

  @Override
  public boolean merge(Frame<? extends FlowValue> frame, Interpreter<FlowValue> interpreter)
      throws AnalyzerException {
    ((FlowInterpreter) interpreter).startMerge(this);
    try {
      return super.merge(frame, interpreter);
    } finally {
      ((FlowInterpreter) interpreter).endMerge();
    }
  }

  ValueReference findReference(FlowValue value) {
    for (int i = 0; i < getStackSize(); i++) {
      if (getStack(i) == value) {
        return ValueReference.stack(this, i);
      }
    }
    for (int i = 0; i < getLocals(); i++) {
      if (getLocal(i) == value) {
        return ValueReference.local(this, i);
      }
    }
    throw new RuntimeException("Cannot find value " + value);
  }

  final Set<FlowValue> getMergedValues(ValueReference ref) {
    Set<FlowValue> result = mergedValues.get(ref);
    if (result == null) {
      result = new HashSet<>();
      mergedValues.put(ref, result);
    }
    return result;
  }
}
