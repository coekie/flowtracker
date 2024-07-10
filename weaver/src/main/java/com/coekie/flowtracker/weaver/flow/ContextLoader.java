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

import com.coekie.flowtracker.tracker.Context;
import com.coekie.flowtracker.weaver.flow.FlowTransformer.FlowMethod;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Manages getting of the {@link Context}. It's gotten only once and stored in a local var as an
 * optimization.
 */
class ContextLoader {
  /** Local variable that holds the {@link Context} */
  private TrackLocal contextLocal;

  /** Ensure that we have stored the Context in {@link #contextLocal} */
  private void ensureLocalPopulated(FlowMethod method) {
    if (contextLocal != null) {
      return;
    }
    contextLocal = method.newLocal(
        Type.getType("Lcom/coekie/flowtracker/tracker/Context;"),
        List.of(
            new MethodInsnNode(Opcodes.INVOKESTATIC,
                "com/coekie/flowtracker/tracker/Context",
                "context",
                "()Lcom/coekie/flowtracker/tracker/Context;",
                false)
        ),
        1,
        "ContextLoader context");
  }

  VarInsnNode load(FlowMethod method) {
    ensureLocalPopulated(method);
    return contextLocal.load();
  }
}
