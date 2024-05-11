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
import com.coekie.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Manages transformation of a called method that may return a tracked value and/or have tracked
 * values as arguments
 *
 * @see InvocationReturnStore
 * @see InvocationArgValue
 * @see InvocationOutgoingTransformation
 */
class InvocationIncomingTransformation {
  /** Local variable that represents the Invocation of the method we're in */
  TrackLocal invocationLocal;

  /** Ensure that we called {@link Invocation#start(String)} at the beginning */
  void ensureStarted(FlowMethodAdapter methodNode) {
    if (invocationLocal != null) {
      return;
    }
    invocationLocal = methodNode.newLocal(
        Type.getType("Lcom/coekie/flowtracker/tracker/Invocation;"),
        List.of(
            new LdcInsnNode(Invocation.signature(methodNode.name, methodNode.desc)),
            new MethodInsnNode(Opcodes.INVOKESTATIC,
            "com/coekie/flowtracker/tracker/Invocation",
            "start",
            "(Ljava/lang/String;)Lcom/coekie/flowtracker/tracker/Invocation;",
            false)
        ),
        "InvocationTransformation invocation");
  }
}
