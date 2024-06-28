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

import com.coekie.flowtracker.tracker.ClassOriginTracker.ClassEntry;
import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

/** Represents an instruction that stores a (possibly tracked) value in an object. */
abstract class Store extends Instrumentable implements FlowValue.FallbackSource {
  final FlowFrame frame;
  final int line;

  Store(FlowFrame frame) {
    this.frame = frame;
    this.line = frame.getLine();
  }

  FlowValue getStackFromTop(int indexFromTop) {
    FlowValue value = frame.getStack(frame.getStackSize() - indexFromTop - 1);
    // this is a bit fragile/awkward. we call getStackFromTop in the constructor of Stores, and then
    // call initCreationFrame from here so that the FlowValue has a chance to find its corresponding
    // frame after analysis is over but before we start adding more instructions (which would make
    // it harder to find the frame back). note that we're just using the frame of the store here to
    // get a reference to the analyzer; the frame that the value was created at is a different
    // frame.
    value.initCreationFrame(frame.analyzer);
    return value;
  }

  @Override
  public void loadSourcePointFallback(InsnList toInsert) {
    FlowMethod method = frame.getMethod();
    ClassEntry fallback = method.constantsTransformation.fallback(method, line);
    ConstantsTransformation.loadClassConstantPoint(toInsert, method, fallback);
  }

  /**
   * Add instructions to `toInsert` to load the source {@link TrackerPoint}.
   * <p>
   * If it's enabled in the config, then when the loaded PointTracker is null, replace it with
   * the fallback (pointing to this Store in the code).
   * we don't do this by default because it's also causing some weird behaviour. But it's useful
   * at least for debugging where we lost track of some value
   */
  void loadSourcePointOrFallback(FlowValue storedValue, InsnList toInsert) {
    storedValue.loadSourcePoint(toInsert, this);

    FlowMethod method = frame.getMethod();
    if (method.useDynamicFallback()
        // don't do it for UntrackableValue, because that is already using the fallback
        && !(storedValue instanceof UntrackableValue)) {
      loadSourcePointFallback(toInsert);
      toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Objects",
          "requireNonNullElse", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"));
      toInsert.add(new TypeInsnNode(Opcodes.CHECKCAST,
          "com/coekie/flowtracker/tracker/TrackerPoint"));
    }
  }

  boolean shouldTrack(FlowValue v) {
    // a more conservative approach (which we used to do) here would be:
    //return v.isTrackable();
    // but to see where in code untracked values come from (fallback), we load sources even of
    // values that are not trackable.
    return true;
  }
}
