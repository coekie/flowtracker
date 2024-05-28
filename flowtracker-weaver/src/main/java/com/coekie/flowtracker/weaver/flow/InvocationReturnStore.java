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
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** The returning of a value of a method that may be instrumented with {@link Invocation} */
class InvocationReturnStore extends Store {
  private final InsnNode returnInsn;
  private final InvocationIncomingTransformation invocation;
  private final FlowValue returnedValue = getStackFromTop(0);

  private InvocationReturnStore(InsnNode returnInsn, FlowFrame frame,
      InvocationIncomingTransformation invocation) {
    super(frame);
    this.returnInsn = returnInsn;
    this.invocation = invocation;
  }

  @Override
  void instrument(FlowMethodAdapter methodNode) {
    // if we know where the value we are returning came from
    if (returnedValue.isTrackable()) {
      InsnList toInsert = new InsnList();
      methodNode.addComment(toInsert, "begin InvocationReturnStore.insertTrackStatements");

      returnedValue.ensureTracked();
      invocation.ensureStarted(methodNode);

      toInsert.add(invocation.invocationLocal.load());
      returnedValue.loadSourcePoint(toInsert);

      toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
          "com/coekie/flowtracker/tracker/Invocation", "returning",
          "(Lcom/coekie/flowtracker/tracker/Invocation;"
              + "Lcom/coekie/flowtracker/tracker/TrackerPoint;)V",
          false));

      methodNode.addComment(toInsert, "end InvocationReturnStore.insertTrackStatements");
      methodNode.maxStack = Math.max(frame.fullStackSize() + 3, methodNode.maxStack);

      methodNode.instructions.insertBefore(returnInsn, toInsert);
    }
  }

  /** Add a {@link InvocationReturnStore} to `toInstrument` when we need to instrument it */
  static void analyze(List<Instrumentable> toInstrument, InsnNode insn, FlowFrame frame,
      FlowMethodAdapter method) {
    if (InvocationReturnValue.shouldInstrumentInvocation(method.name, method.desc)) {
      toInstrument.add(new InvocationReturnStore(insn, frame, method.invocation));
    }
  }
}
