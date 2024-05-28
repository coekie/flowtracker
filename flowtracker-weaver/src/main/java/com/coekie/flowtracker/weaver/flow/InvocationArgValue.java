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

import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethodAdapter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** Value passed as an argument in a method that we track using {@link Invocation} */
class InvocationArgValue extends TrackableValue {
  private final int argNum;
  InvocationArgValue(FlowMethodAdapter flowMethodAdapter, AbstractInsnNode insn, int argNum) {
    super(flowMethodAdapter, Type.getArgumentTypes(flowMethodAdapter.desc)[argNum], insn);
    if (argNum > InvocationArgStore.MAX_ARG_NUM_TO_INSTRUMENT + 1) {
      throw new IllegalArgumentException();
    }
    this.argNum = argNum;
  }

  @Override void insertTrackStatements() {
    flowMethodAdapter.invocation.ensureStarted(flowMethodAdapter);
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert, "InvocationArgValue.loadSourcePoint");
    toInsert.add(flowMethodAdapter.invocation.invocationLocal.load());
    toInsert.add(new InsnNode(Opcodes.ICONST_0 + argNum));
    toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
        "com/coekie/flowtracker/tracker/Invocation",
        "getArgPoint",
        "(Lcom/coekie/flowtracker/tracker/Invocation;I)"
            + "Lcom/coekie/flowtracker/tracker/TrackerPoint;"));
  }
}
