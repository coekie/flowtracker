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

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

/**
 * A value received from a cast operation between a long and an int (int/char/byte/short).
 */
class CastValue extends FlowValue {
  private final FlowValue target;
  private final AbstractInsnNode insn;

  CastValue(Type type, AbstractInsnNode insn, FlowValue target) {
    super(type);
    this.insn = insn;
    this.target = target;
  }

  @Override
  boolean isTrackable() {
    return target.isTrackable();
  }

  @Override
  AbstractInsnNode getCreationInsn() {
    return insn;
  }

  @Override
  boolean hasCreationInsn() {
    return true;
  }

  @Override
  void ensureTracked() {
    target.ensureTracked();
  }

  @Override
  void loadSourcePoint(InsnList toInsert, FallbackSource fallback) {
    target.loadSourcePoint(toInsert, fallback);
  }
}
