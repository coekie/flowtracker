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

import com.coekie.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
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

  TesterStore(MethodInsnNode invokeInsn, FlowFrame frame, int valueStackIndexFromTop) {
    super(frame);
    this.invokeInsn = invokeInsn;
    this.storedValue = getStackFromTop(valueStackIndexFromTop);
  }

  void insertTrackStatements(FlowMethodAdapter methodNode) {
    if (storedValue.isTrackable()) { // if we know where the value we are storing came from
      storedValue.ensureTracked();

      // replace the call with a call to the $tracked_ method, with two extra arguments: the tracker
      // and the index
      InsnList toInsert = new InsnList();
      methodNode.addComment(toInsert, "begin TesterStore.insertTrackStatements");

      storedValue.loadSourcePoint(toInsert);

      methodNode.addComment(toInsert,
          "end TesterStore.insertTrackStatements. also replaced next invocation");

      methodNode.maxStack = Math.max(frame.fullStackSize() + 3, methodNode.maxStack);

      invokeInsn.name = "$tracked_" + invokeInsn.name;
      invokeInsn.desc =
          invokeInsn.desc.replace(")", "Lcom/coekie/flowtracker/tracker/TrackerPoint;)");

      methodNode.instructions.insertBefore(invokeInsn, toInsert);
    }
  }
}