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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;

/** Represents an instruction that stores a (possibly tracked) value in an object. */
abstract class Store extends Instrumentable implements FlowValue.FallbackSource {
  final FlowFrame frame;

  Store(FlowFrame frame) {
    this.frame = frame;
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
    toInsert.add(new InsnNode(Opcodes.ACONST_NULL));
  }
}
