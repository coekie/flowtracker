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

import com.coekie.flowtracker.tracker.TrackerPoint;
import java.util.Objects;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.analysis.BasicValue;

/**
 * A value in our flow analysis. That is, a representation of what we know about a certain value
 * (on the stack or in a local variable) at a certain point in the execution of a method.
 */
abstract class FlowValue extends BasicValue {
  private FlowFrame creationFrame;

  FlowValue(Type type) {
    super(type);
  }

  /**
   * Insert the statements needed to keep track of the origin of this value, if applicable.
   * Idempotent: may be called multiple times; tracking should only be setup once.
   */
  abstract void ensureTracked();

  /**
   * Returns if we can know where this value came from.
   * <p>
   * If this is false, then {@link #loadSourcePoint(InsnList, FallbackSource)} will always load
   * {@code null}.
   * If this return true, then that's no guarantee that
   * {@link #loadSourcePoint(InsnList, FallbackSource)} is never {@code null}.
   */
  abstract boolean isTrackable();

  /**
   * The instruction at which this value was last touched (created, copied or merged), or null if
   * unknown.
   */
  abstract AbstractInsnNode getCreationInsn();

  /**
   * The {@link FlowFrame} that the analyzer built for {@link #getCreationInsn()}
   */
  FlowFrame getCreationFrame() {
    return creationFrame;
  }

  void initCreationFrame(FlowAnalyzer analyzer) {
    AbstractInsnNode creationInsn = getCreationInsn();
    if (creationInsn != null) {
      creationFrame = analyzer.getFrame(creationInsn);
    }
  }

  /**
   * Add the {@link TrackerPoint} from which this value came on top of the stack.
   * The instructions inserted should use maximum 2 stack entries.
   *
   * @param toInsert list of instructions where the needed statements are added to at the end
   */
  abstract void loadSourcePoint(InsnList toInsert, FallbackSource fallback);

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o.getClass() != this.getClass()) {
      return false;
    } else {
      return Objects.equals(getType(), ((FlowValue) o).getType());
    }
  }

  @Override
  public int hashCode() {
    return getType() == null ? 0 : getType().hashCode();
  }

  /**
   * Returns if this value somewhere (through merges and copies) contains a MergeValue at the given
   * mergingFrame. Used to detect loops in the data flow.
   */
  abstract boolean hasMergeAt(FlowFrame mergingFrame);

  /**
   * Fallback for {@link #loadSourcePoint}. If we're not tracking the source of a value,
   * we can use this as a source instead, so that instead we point to where in the code this value
   * first started to get tracked.
   * <p>
   * This fallback is only applicable to UntrackedValue, not to a TrackableValue with a null value.
   * In other words, if we use the fallback is determined statically at code analysis time, not
   * dynamically based on runtime values.
   * So e.g. a value returned from a method invocation for a method that's not tracked will use this
   * fallback (it comes directly from something that's not tracked), but getting a value out of a
   * byte[] where the byte[] instance hasn't got a tracker does not use the fallback.
   * (We could also consider such a dynamic fallback in the future).
   */
  interface FallbackSource {
    void loadSourcePointFallback(InsnList toInsert);
  }

  enum NullFallbackSource implements FallbackSource {
    INSTANCE;

    @Override
    public void loadSourcePointFallback(InsnList toInsert) {
      toInsert.add(new InsnNode(Opcodes.ACONST_NULL));
    }
  }
}
