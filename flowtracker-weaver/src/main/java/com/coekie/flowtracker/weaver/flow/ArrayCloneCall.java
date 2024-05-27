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

import com.coekie.flowtracker.hook.ArrayHook;
import com.coekie.flowtracker.weaver.flow.FlowAnalyzingTransformer.FlowMethodAdapter;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * A call to .clone() on a primitive array, that we replace by a call to {@link ArrayHook}
 */
class ArrayCloneCall extends Instrumentable {
  private final MethodInsnNode mInsn;

  private ArrayCloneCall(MethodInsnNode mInsn) {
    this.mInsn = mInsn;
  }

  @Override
  void instrument(FlowMethodAdapter methodNode) {
    mInsn.desc = '(' + mInsn.owner + ')' + mInsn.owner;
    mInsn.owner = "com/coekie/flowtracker/hook/ArrayHook";
    mInsn.setOpcode(Opcodes.INVOKESTATIC);
  }

  /** Add a {@link ArrayCloneCall} to `toInstrument` when we need to instrument it */
  static boolean analyze(List<Instrumentable> toInstrument, MethodInsnNode mInsn) {
    if ("clone".equals(mInsn.name)
        && (mInsn.owner.equals("[C") || mInsn.owner.equals("[B")
        || mInsn.owner.equals("[I"))) {
      toInstrument.add(new ArrayCloneCall(mInsn));
      return true;
    }
    return false;
  }
}
