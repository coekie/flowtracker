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
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Manages transformation of a method call for which we may track the return value and/or arguments.
 *
 * @see InvocationArgStore
 * @see InvocationReturnValue
 * @see InvocationIncomingTransformation
 */
class InvocationOutgoingTransformation {
  private final MethodInsnNode mInsn;
  private final FlowMethod methodNode;

  /**
   * The instruction that creates the Invocation. Initially this is a call to
   * {@link Invocation#createCalling(String)}. If necessary this is split into invocations to
   * {@link Invocation#create(String)} and {@link Invocation#calling()}. (Only splitting when
   * necessary, to minimize size of the instrumented code).
   */
  private MethodInsnNode createInsn;

  /** Call to {@link Invocation#calling()}, if has been split from {@link #createInsn} */
  private MethodInsnNode callingInsn;

  /** Last instruction of our instrumentation, that removes the {@link Invocation} from the stack */
  private AbstractInsnNode endInsn;

  InvocationOutgoingTransformation(MethodInsnNode mInsn, FlowMethod methodNode) {
    this.mInsn = mInsn;
    this.methodNode = methodNode;
  }

  /** Insert call to {@link Invocation#createCalling(String)} if it hasn't been added yet */
  void ensureInstrumented() {
    if (endInsn != null) {
      return;
    }

    InsnList toInsert = new InsnList();
    methodNode.addComment(toInsert, "begin InvocationOutgoingTransformation.ensureInstrumented");
    toInsert.add(new LdcInsnNode(Invocation.signature(mInsn.name, mInsn.desc)));
    createInsn = new MethodInsnNode(Opcodes.INVOKESTATIC,
        "com/coekie/flowtracker/tracker/Invocation",
        "createCalling",
        "(Ljava/lang/String;)Lcom/coekie/flowtracker/tracker/Invocation;");
    toInsert.add(createInsn);
    // initially we assume we're not going to need the Invocation anymore.
    endInsn = new InsnNode(Opcodes.POP);
    toInsert.add(endInsn);

    methodNode.addComment(toInsert, "end InvocationOutgoingTransformation.ensureInstrumented");
    methodNode.instructions.insertBefore(mInsn, toInsert);
  }

  /**
   * Insert instructions that operate on the Invocation, before the actual method invocation. At
   * this point the {@link Invocation} is on top of the stack, and at the end of the inserted
   * instructions it should still be there.
   */
  void insertInvocationPreparation(InsnList toInsert) {
    if (callingInsn == null) {
      // split call to "createCalling" into "create" and "calling"; where we can put the `toInsert`
      // in between.
      createInsn.name = "create";
      callingInsn = new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
          "com/coekie/flowtracker/tracker/Invocation",
          "calling",
          "()Lcom/coekie/flowtracker/tracker/Invocation;");
      methodNode.instructions.insertBefore(endInsn, callingInsn);
    }

    methodNode.instructions.insertBefore(callingInsn, toInsert);
  }

  /**
   * Instead for POP'ing the {@link Invocation} at the end, store it in the given local var.
   */
  void storeInvocation(TrackLocal invocationLocal) {
    if (endInsn == null || endInsn.getOpcode() != Opcodes.POP) {
      throw new IllegalStateException();
    }
    VarInsnNode newEndInsn = invocationLocal.store();
    methodNode.instructions.set(endInsn, newEndInsn);
    endInsn = newEndInsn;
  }
}
