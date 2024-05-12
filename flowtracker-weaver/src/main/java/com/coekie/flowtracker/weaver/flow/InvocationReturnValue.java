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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

/** Value returned from a called method, that we track using {@link Invocation} */
class InvocationReturnValue extends TrackableValue {
  final InvocationOutgoingTransformation transformation;

  /** Local variable storing the Invocation */
  private TrackLocal invocationLocal;

  InvocationReturnValue(FlowMethodAdapter flowMethodAdapter, MethodInsnNode mInsn) {
    super(flowMethodAdapter, Type.getReturnType(mInsn.desc), mInsn);
    this.transformation = new InvocationOutgoingTransformation(mInsn, flowMethodAdapter);
  }

  @Override void insertTrackStatements() {
    invocationLocal = flowMethodAdapter.newLocalForObject(
        Type.getType("Lcom/coekie/flowtracker/tracker/Invocation;"),
        "InvocationReturnValue invocation");

    transformation.ensureInstrumented();
    transformation.storeInvocation(invocationLocal);

    // ensureInstrumented puts one extra value on the stack
    flowMethodAdapter.maxStack = Math.max(flowMethodAdapter.maxStack,
        getCreationFrame().fullStackSize() + 1);
  }

  @Override
  void loadSourcePoint(InsnList toInsert) {
    flowMethodAdapter.addComment(toInsert, "InvocationReturnValue.loadSourcePoint");
    toInsert.add(invocationLocal.load());
    toInsert.add(new FieldInsnNode(Opcodes.GETFIELD,
        "com/coekie/flowtracker/tracker/Invocation",
        "returnPoint",
        "Lcom/coekie/flowtracker/tracker/TrackerPoint;"));
  }

  static boolean shouldInstrumentInvocation(String name, String desc) {
    Type returnType = Type.getReturnType(desc);
    if (returnType.equals(Type.BYTE_TYPE) || returnType.equals(Type.CHAR_TYPE)) {
      return true;
    }

    if (returnType.equals(Type.INT_TYPE)) {
      // heuristic guessing which methods are worth tracking the return value of, because that
      // probably is a char or byte read from somewhere
      if ((name.contains("read") || name.contains("Read"))
          // don't instrument methods where the output is going through a buffer passed into it as
          // parameter. for those, the returned value is probably the length
          && !desc.contains("[") && !desc.contains("Buffer")) {
        return true;
      }

      // e.g. Character.codePointAt
      return name.contains("codePoint") || name.contains("CodePoint");
    }
    return false;
  }
}