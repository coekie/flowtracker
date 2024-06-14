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
import org.objectweb.asm.tree.analysis.Interpreter;

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
   * <p>
   * Should only be called after analysis has finished (so that
   * {@link FlowFrame#initInsn} has been called). That is a limitation because ASM
   * does not pass along the instruction to the interpreter.
   */
  abstract AbstractInsnNode getCreationInsn();

  /**
   * Return if we will know where FlowValue was created; that is if {@link #getCreationInsn()} will
   * return a non-null value after analysis.
   */
  abstract boolean hasCreationInsn();

  /**
   * The {@link FlowFrame} that the analyzer built for {@link #getCreationInsn()}
   */
  FlowFrame getCreationFrame() {
    return creationFrame;
  }

  boolean initCreationFrame(FlowAnalyzer analyzer) {
    if (creationFrame == null) {
      AbstractInsnNode creationInsn = getCreationInsn();
      if (creationInsn != null) {
        creationFrame = analyzer.getFrame(creationInsn);
        return true;
      }
    }
    return false;
  }

  /**
   * Add the {@link TrackerPoint} from which this value came on top of the stack.
   * The instructions inserted should use maximum 2 stack entries.
   *
   * @param toInsert list of instructions where the needed statements are added to at the end
   */
  abstract void loadSourcePoint(InsnList toInsert, FallbackSource fallback);

  /**
   * Combine `this` and `other` into a single FlowValue, if the combination can be represented with
   * a single FlowValue. This is meant for handling merges ({@link Interpreter#merge} where we have
   * two different values because the same instructions were interpreted multiple times (as opposed
   * to a merge that happens because of converging control flow).
   * You could say this is handling the "simple" case of the merge.
   * <p>
   * Concretely, this handles one round of interpretation having seen that there's a MergeValue, and
   * another interpretation just having one of those values, so effectively being a _subset_ of it.
   * So e.g. `value1.mergeInPlace(MergeValue1)`, where MergeValue1 already contains value1, returns
   * MergeValue1. And this recurses, so that e.g.
   * CopyValue(value1).mergeInPLace(CopyValue(MergeValue1))` (where the two CopyValues are caused by
   * the same instruction) also returns CopyOf(MergeValue1).
   */
  final FlowValue mergeInPlace(FlowValue other) {
    // if one of them is a MergedValue, let that one handle it
    if (this instanceof MergedValue) {
      return doMergeInPlace(other);
    } else if (other instanceof MergedValue) {
      return other.doMergeInPlace(this);
    } else if (this.getClass() != other.getClass()) {
      // except for MergedValue, merging in place can only happen for two values of the same type
      return null;
    } else {
      return doMergeInPlace(other);
    }
  }

  /**
   * Implement {@link #mergeInPlace(FlowValue)}, for the non-trivial case.
   * When this is called, `other` is always the same type as `this`.
   */
  abstract FlowValue doMergeInPlace(FlowValue other);

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
