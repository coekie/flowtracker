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

import com.coekie.flowtracker.hook.SystemHook;
import com.coekie.flowtracker.weaver.Types;
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
import java.util.List;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * A call to {@link System#arraycopy(Object, int, Object, int, int)} that we can replace with a call
 * to {@link SystemHook#arraycopy(Object, int, Object, int, int)}
 */
class ArrayCopyCall extends Instrumentable {
  private final MethodInsnNode mInsn;

  private ArrayCopyCall(MethodInsnNode mInsn) {
    this.mInsn = mInsn;
  }

  @Override
  void instrument(FlowMethod methodNode) {
    // our hook method has same name and signature as System.arraycopy, so we only adjust the call
    // to point to our class
    mInsn.owner = "com/coekie/flowtracker/hook/SystemHook";
  }

  /** Add a {@link ArrayCopyCall} to `toInstrument` when we need to instrument it */
  static boolean analyze(List<Instrumentable> toInstrument, MethodInsnNode mInsn, FlowFrame frame) {
    if ("java/lang/System".equals(mInsn.owner)
        && "arraycopy".equals(mInsn.name)
        && "(Ljava/lang/Object;ILjava/lang/Object;II)V".equals(mInsn.desc)) {
      // if it is a copy from char[] to char[] or from byte[] to byte[]
      Type sourceType = frame.getStack(frame.getStackSize() - 5).getType();
      Type destType = frame.getStack(frame.getStackSize() - 3).getType();
      if ((Types.CHAR_ARRAY.equals(sourceType) && Types.CHAR_ARRAY.equals(destType))
          || (Types.BYTE_ARRAY.equals(sourceType) && Types.BYTE_ARRAY.equals(destType))) {
        // replace it with a call to our hook
        toInstrument.add(new ArrayCopyCall(mInsn));
        return true;
      }
    }
    return false;
  }
}
