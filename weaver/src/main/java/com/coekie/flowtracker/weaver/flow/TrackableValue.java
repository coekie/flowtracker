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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;

/** A value of which we can track where it came from */
abstract class TrackableValue extends FlowValue {
  final FlowMethod method;
  private final AbstractInsnNode insn;
  private boolean tracked;

  TrackableValue(FlowMethod method, Type type, AbstractInsnNode insn) {
    super(type);
    this.method = method;
    this.insn = insn;
  }

  @Override
  boolean isTrackable() {
    return true;
  }

  @Override
  final void ensureTracked() {
    if (!tracked) {
      insertTrackStatements();
      tracked = true;
    }
  }

  @Override
  AbstractInsnNode getCreationInsn() {
    return insn;
  }

  @Override
  boolean hasCreationInsn() {
    return true;
  }

  /**
   * Insert the statements needed to keep track of the origin of this value.
   * <p>
   * This method should not be called directly, instead {@link #ensureTracked()}
   * should be used, to ensure statements are not inserted more than once.
   */
  abstract void insertTrackStatements();

  @Override
  public boolean equals(Object o) {
    return o == this || (super.equals(o) && ((TrackableValue) o).insn == this.insn);
  }

  @Override
  FlowValue doMergeInPlace(FlowValue other) {
    if (this == other) {
      return this;
    } else {
      return null;
    }
  }
}
