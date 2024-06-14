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

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

/**
 * A value that got copied (e.g. put in a local variable, or loaded onto the stack). We use this to
 * find which instructions a value came from in {@link MergedValue}.
 */
class CopyValue extends FlowValue {
  private final FlowValue original;
  private final AbstractInsnNode insn;

  CopyValue(FlowValue original, AbstractInsnNode insn) {
    super(original.getType());
    this.original = original;
    this.insn = insn;
  }

  FlowValue getOriginal() {
    return original;
  }

  @Override
  void ensureTracked() {
    original.ensureTracked();
  }

  @Override
  boolean isTrackable() {
    return original.isTrackable();
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
  boolean initCreationFrame(FlowAnalyzer analyzer) {
    if (super.initCreationFrame(analyzer)) {
      original.initCreationFrame(analyzer);
      return true;
    } else {
      return false;
    }
  }

  @Override
  void loadSourcePoint(InsnList toInsert, FallbackSource fallback) {
    original.loadSourcePoint(toInsert, fallback);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!super.equals(o)) {
      return false;
    }
    CopyValue other = (CopyValue) o;
    return other.insn == insn && other.original.equals(original);
  }
}
