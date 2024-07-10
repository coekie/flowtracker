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
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** A value received from a {@code FlowTester.createSource*()}. */
class TesterValue extends TrackableValue {
  /** The call to FlowTester.createSource*() */
  private final MethodInsnNode mInsn;

  /** Local variable storing the target FlowTester */
  private TrackLocal testerLocal;

  boolean isLong;

  TesterValue(FlowMethod method, MethodInsnNode mInsn, boolean isLong) {
    super(method, Type.getReturnType(mInsn.desc), mInsn);
    this.mInsn = mInsn;
    this.isLong = isLong;
  }

  @Override void insertTrackStatements() {
    // on the stack before the call: FlowTester tester, char c
    testerLocal = method.newLocalForObject(
        Type.getObjectType("com/coekie/flowtracker/test/FlowTester"),
        "TesterValue tester");

    InsnList toInsert = new InsnList();

    // store tester. doing a little dance on the stack because tester isn't on top.
    if (isLong) {
      // stack: tester, l
      toInsert.add(new InsnNode(Opcodes.DUP2_X1)); // dup l down the stack
      // stack: l, tester, l
      toInsert.add(new InsnNode(Opcodes.POP2)); // pop l
      // stack: l, tester
      toInsert.add(new InsnNode(Opcodes.DUP_X2)); // dup tester down the stack
      // stack: tester, l, tester
      toInsert.add(testerLocal.store()); // store tester
      // stack: tester, l
    } else {
      // stack: tester, c
      toInsert.add(new InsnNode(Opcodes.DUP2)); // dup tester and c
      // stack: tester, c, tester, c
      toInsert.add(new InsnNode(Opcodes.POP)); // pop c
      // stack: tester, c, tester
      toInsert.add(testerLocal.store()); // store tester
      // stack: tester, c
    }

    mInsn.name = "$tracked_" + mInsn.name;

    method.instructions.insertBefore(mInsn, toInsert);
  }

  @Override
  void loadSourcePoint(InsnList toInsert, FallbackSource fallback) {
    method.addComment(toInsert,
        "TesterValue.loadSourcePoint: testerLocal.theSourcePoint()");
    toInsert.add(testerLocal.load());
    toInsert.add(
        new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
            "com/coekie/flowtracker/test/FlowTester",
            "point",
            "()Lcom/coekie/flowtracker/tracker/TrackerPoint;"));
  }
}
