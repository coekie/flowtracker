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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * A value produced from combining two values using binary or (|).
 * An OrValue is only created in the non-trivial case: combining two non-constant trackable values.
 */
class OrValue extends FlowValue {
  private final FlowMethod method;
  private final AbstractInsnNode insn;
  private final FlowValue target1;
  private final FlowValue target2;

  OrValue(FlowMethod method, Type type, AbstractInsnNode insn,
      FlowValue target1, FlowValue target2) {
    super(type);
    this.method = method;
    this.insn = insn;
    this.target1 = target1;
    this.target2 = target2;
    if (!target1.isTrackable() || !target2.isTrackable()) {
      throw new IllegalArgumentException();
    }
  }

  @Override
  boolean isTrackable() {
    return true;
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
      target1.initCreationFrame(analyzer);
      target2.initCreationFrame(analyzer);
      return true;
    } else {
      return false;
    }
  }

  @Override
  void ensureTracked() {
    target1.ensureTracked();
    target2.ensureTracked();
  }

  @Override
  void loadSourcePoint(InsnList toInsert, FallbackSource fallback) {
    // for now loadSourcePoint is only allowed to use two values on the stack.
    // so we use a temporary variable to avoid using more than that.

    TrackLocal pointTrackerLocal = method.newLocalForObject(
        Type.getType("Lcom/coekie/flowtracker/tracker/TrackerPoint;"),
        "OrValue tmp PointTracker");
    target2.loadSourcePoint(toInsert, fallback);
    toInsert.add(pointTrackerLocal.store());
    target1.loadSourcePoint(toInsert, fallback);
    toInsert.add(pointTrackerLocal.load());
    toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/coekie/flowtracker/hook/OrHook",
        "or",
        "(Lcom/coekie/flowtracker/tracker/TrackerPoint;"
            + "Lcom/coekie/flowtracker/tracker/TrackerPoint;)"
            + "Lcom/coekie/flowtracker/tracker/TrackerPoint;"));
  }

  @Override
  FlowValue doMergeInPlace(FlowValue o) {
    OrValue other = (OrValue) o;
    if (this.insn == other.insn) {
      FlowValue mergedTarget1 = this.target1.mergeInPlace(other.target1);
      FlowValue mergedTarget2 = this.target2.mergeInPlace(other.target2);
      if (mergedTarget1 == this.target1 && mergedTarget2 == this.target2) {
        return this;
      } else if (mergedTarget1 == other.target1 && mergedTarget2 == other.target2) {
        return other;
      } else if (mergedTarget1 == null || mergedTarget2 == null) {
        return null;
      } else {
        return new OrValue(method, getType(), insn, mergedTarget1, mergedTarget2);
      }
    } else {
      return null;
    }
  }
}
