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
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

class ConstantValue extends TrackableValue {
  private final int value;
  private final int line;
  private ClassConstant constant;

  ConstantValue(FlowMethod method, Type type, AbstractInsnNode insn, int value) {
    super(method, type, insn);
    this.value = value;
    this.line = method.getLine(insn);
  }

  @Override
  void insertTrackStatements() {
    constant = method.constantsTransformation.trackConstant(method, value, line);
  }

  @Override
  void loadSourcePoint(InsnList toInsert, FallbackSource fallback) {
    ConstantsTransformation.loadClassConstantPoint(toInsert, method, constant);
  }
}
