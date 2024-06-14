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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.analysis.BasicValue;

/** Value for we can't track where it came from */
class UntrackableValue extends FlowValue {
  /**
   * An uninitialized value.
   * @see BasicValue#UNINITIALIZED_VALUE
   */
  static final FlowValue UNINITIALIZED_VALUE = new UntrackableValue(null);

  /** A byte, boolean, char, short, or int value. */
  static final FlowValue INT_VALUE = new UntrackableValue(Type.INT_TYPE);

  /** A float value. */
  static final FlowValue FLOAT_VALUE = new UntrackableValue(Type.FLOAT_TYPE);

  /** A long value. */
  static final FlowValue LONG_VALUE = new UntrackableValue(Type.LONG_TYPE);

  /** A double value. */
  static final FlowValue DOUBLE_VALUE = new UntrackableValue(Type.DOUBLE_TYPE);

  /** An object or array reference value. */
  static final FlowValue REFERENCE_VALUE = new UntrackableValue(Types.OBJECT);

  /** A void value (?!?), used at least for representing return address for JSR instructions */
  static final FlowValue VOID_VALUE = new UntrackableValue(Type.VOID_TYPE);

  UntrackableValue(Type type) {
    super(type);
  }

  @Override
  boolean isTrackable() {
    return false;
  }

  @Override
  void ensureTracked() {
  }

  @Override
  AbstractInsnNode getCreationInsn() {
    return null;
  }

  @Override
  boolean hasCreationInsn() {
    return false;
  }

  @Override
  void loadSourcePoint(InsnList toInsert, FallbackSource fallback) {
    fallback.loadSourcePointFallback(toInsert);
  }

  @Override
  boolean hasMergeAt(FlowFrame mergingFrame) {
    return false;
  }
}
