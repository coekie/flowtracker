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
import java.util.List;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Invocation of a method on FlowTester that receives a tracked value.
 * For testing only.
 */
// on the stack: value, ...
class TesterStore extends Store {
  private final MethodInsnNode invokeInsn;
  private final FlowValue storedValue;

  private TesterStore(MethodInsnNode invokeInsn, FlowFrame frame, int valueStackIndexFromTop) {
    super(frame);
    this.invokeInsn = invokeInsn;
    this.storedValue = getStackFromTop(valueStackIndexFromTop);
  }

  void instrument(FlowMethod methodNode) {
    if (storedValue.isTrackable()) { // if we know where the value we are storing came from
      storedValue.ensureTracked();

      // replace the call with a call to the $tracked_ method, with one extra argument: TrackerPoint
      InsnList toInsert = new InsnList();
      methodNode.addComment(toInsert, "begin TesterStore.instrument");

      storedValue.loadSourcePoint(toInsert, this);

      methodNode.addComment(toInsert,
          "end TesterStore.insertTrackStatements. also replaced next invocation");

      methodNode.maxStack = Math.max(frame.fullStackSize() + 3, methodNode.maxStack);

      invokeInsn.name = "$tracked_" + invokeInsn.name;
      invokeInsn.desc =
          invokeInsn.desc.replace(")", "Lcom/coekie/flowtracker/tracker/TrackerPoint;)");

      methodNode.instructions.insertBefore(invokeInsn, toInsert);
    }
  }

  /** Add a {@link TesterStore} to `toInstrument` when we need to instrument it */
  static boolean analyze(List<Instrumentable> toInstrument, MethodInsnNode mInsn, FlowFrame frame) {
    if (mInsn.owner.equals("com/coekie/flowtracker/test/FlowTester")) {
      if (mInsn.name.equals("assertTrackedValue")) {
        toInstrument.add(new TesterStore(mInsn, frame, 3));
        return true;
      } else if (mInsn.name.equals("assertIsTheTrackedValue")
          || mInsn.name.equals("getCharSourceTracker")
          || mInsn.name.equals("getCharSourcePoint")
          || mInsn.name.equals("getByteSourceTracker")
          || mInsn.name.equals("getByteSourcePoint")
          || mInsn.name.equals("getIntSourcePoint")) {
        toInstrument.add(new TesterStore(mInsn, frame, 0));
        return true;
      }
    }
    return false;
  }
}
