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
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
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

  InvocationReturnValue(FlowMethod method, MethodInsnNode mInsn) {
    super(method, Type.getReturnType(mInsn.desc), mInsn);
    this.transformation = new InvocationOutgoingTransformation(mInsn, method);
  }

  @Override void insertTrackStatements() {
    invocationLocal = method.newLocalForObject(
        Type.getType("Lcom/coekie/flowtracker/tracker/Invocation;"),
        "InvocationReturnValue invocation");

    transformation.ensureInstrumented();
    transformation.storeInvocation(invocationLocal);

    // ensureInstrumented puts two extra value on the stack
    method.maxStack = Math.max(method.maxStack,
        getCreationFrame().fullStackSize() + 2);
  }

  @Override
  void loadSourcePoint(InsnList toInsert, FallbackSource fallback) {
    method.addComment(toInsert, "InvocationReturnValue.loadSourcePoint");
    toInsert.add(invocationLocal.load());
    toInsert.add(new FieldInsnNode(Opcodes.GETFIELD,
        "com/coekie/flowtracker/tracker/Invocation",
        "returnPoint",
        "Lcom/coekie/flowtracker/tracker/TrackerPoint;"));
  }

  static boolean shouldInstrumentInvocation(String owner, String name, String desc) {
    Type returnType = Type.getReturnType(desc);
    if (returnType.equals(Type.BYTE_TYPE) || returnType.equals(Type.CHAR_TYPE)) {

      // exclude String.coder() and AbstractStringBuilder.getCoder() for performance and
      // circularity problems
      if (name.equals("coder") || name.equals("getCoder")) {
        return false;
      } else if ((owner.equals("java/lang/String") || owner.equals("java/lang/StringLatin1"))
          && name.equals("charAt")) {
        return false; // handled by CharAtValue
      } else if (owner.startsWith("java/lang/invoke/VarHandle")) {
        return false; // optimization, tracking through VarHandles doesn't work anyway
      }

      return true;
    }

    if (returnType.equals(Type.INT_TYPE)) {
      // heuristic guessing which methods are worth tracking the return value of, because that
      // probably is a char or byte read from somewhere
      // (but avoiding ConcurrentHashMap.spread because of circularity)
      if ((name.startsWith("read") || name.contains("Read"))
          // don't instrument methods where the output is going through a buffer passed into it as
          // parameter. for those, the returned value is probably the length
          && !desc.contains("[") && !desc.contains("Buffer")) {
        return true;
      }

      if (name.startsWith("encode") || name.startsWith("decode")) {
        return true;
      }

      if (InvocationArgStore.eagerFilter != null && InvocationArgStore.eagerFilter.include(owner)) {
        return true;
      }

      // e.g. Character.codePointAt.
      // but not [String|Character].offsetByCodePoints because that's useless and seen in profiler
      // as significant overhead (as used by CheckMethodAdapter).
      return name.contains("codePoint")
          || (name.contains("CodePoint") && !name.startsWith("offset"));
    }
    return false;
  }

  /**
   * Returns true if when invoking the given method, we consider the return value the same as the
   * first argument, skipping invocation instrumentation.
   */
  static boolean passThroughInvocation(String owner, String methodName) {
    switch (owner) {
      case "java/lang/Byte":
        if ("toUnsignedInt".equals(methodName)) {
          return true;
        }
        break;
      case "java/lang/Character":
        if ("toUpperCase".equals(methodName) || "toLowerCase".equals(methodName)
            || "toTitleCase".equals(methodName)) {
          return true;
        }
        break;
    }
    return false;
  }
}
