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

import com.coekie.flowtracker.hook.ReflectionHook;
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethodAdapter;
import java.lang.reflect.Method;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * A call to {@link Method#getName()} that we can replace with a call to {@link ReflectionHook}
 * <p>
 * Similar to {@link ClassNameCall} and {@link FieldNameCall}.
 */
class MethodNameCall extends Instrumentable {
  private final MethodInsnNode mInsn;

  private MethodNameCall(MethodInsnNode mInsn) {
    this.mInsn = mInsn;
  }

  @Override
  void instrument(FlowMethodAdapter methodNode) {
    ConstantsTransformation constantsTransformation = methodNode.constantsTransformation;
    if (constantsTransformation.canBreakStringInterning()) {
      mInsn.desc = "(Ljava/lang/reflect/Method;)Ljava/lang/String;";
      mInsn.name = "getMethodName";
      mInsn.owner = "com/coekie/flowtracker/hook/ReflectionHook";
      mInsn.setOpcode(Opcodes.INVOKESTATIC);
    }
  }

  /** Add a {@link MethodNameCall} to `toInstrument` when we need to instrument it */
  static boolean analyze(List<Instrumentable> toInstrument, MethodInsnNode mInsn) {
    if ("java/lang/reflect/Method".equals(mInsn.owner)
        && "getName".equals(mInsn.name)
        && "()Ljava/lang/String;".equals(mInsn.desc)) {
      toInstrument.add(new MethodNameCall(mInsn));
      return true;
    }
    return false;
  }
}
