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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Instruments an invocation of {@link sun.misc.Unsafe#putByte(Object, long, byte)}.
 * We could also Unsafe methods; but for now this is the only one we care about (motivated by how
 * google protobuf uses it).
 */
// on the stack: Unsafe, Object (maybe/usually byte[]), offset, value
class UnsafeStore extends Store {
  private final MethodInsnNode invokeInsn;
  private final FlowValue storedValue;

  private UnsafeStore(MethodInsnNode invokeInsn, FlowFrame frame) {
    super(frame);
    this.invokeInsn = invokeInsn;
    this.storedValue = getStackFromTop(0);
  }

  void instrument(FlowMethod methodNode) {
    if (storedValue.isTrackable()) { // if we know where the value we are storing came from
      storedValue.ensureTracked();

      // replace the call with a call to UnsafeHook, with one extra argument: the TrackerPoint
      InsnList toInsert = new InsnList();
      methodNode.addComment(toInsert, "begin UnsafeStore.instrument");

      storedValue.loadSourcePoint(toInsert, this);

      methodNode.addComment(toInsert,
          "end UnsafeStore.instrument. also replaced next invocation");

      methodNode.maxStack = Math.max(frame.fullStackSize() + 2, methodNode.maxStack);

      invokeInsn.owner = "com/coekie/flowtracker/hook/UnsafeHook";
      invokeInsn.setOpcode(Opcodes.INVOKESTATIC);
      invokeInsn.desc = "(Lsun/misc/Unsafe;Ljava/lang/Object;JB"
          + "Lcom/coekie/flowtracker/tracker/TrackerPoint;)V";

      methodNode.instructions.insertBefore(invokeInsn, toInsert);
    }
  }

  /** Add a {@link UnsafeStore} to `toInstrument` when we need to instrument it */
  static boolean analyze(List<Instrumentable> toInstrument, MethodInsnNode mInsn, FlowFrame frame) {
    if (mInsn.owner.equals("sun/misc/Unsafe") && mInsn.name.equals("putByte")
        && mInsn.desc.equals("(Ljava/lang/Object;JB)V")) {
      toInstrument.add(new UnsafeStore(mInsn, frame));
      return true;
    }
    return false;
  }
}
