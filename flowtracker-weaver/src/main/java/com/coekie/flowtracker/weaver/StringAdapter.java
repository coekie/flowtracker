package com.coekie.flowtracker.weaver;

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

import com.coekie.flowtracker.hook.StringHook;
import com.coekie.flowtracker.util.Config;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

/**
 * Instrumentation for {@link String}.
 */
class StringAdapter extends ClassVisitor {
  private final boolean debugUntracked;

  public StringAdapter(ClassVisitor cv, Config config) {
    super(Opcodes.ASM9, cv);
    debugUntracked = config.containsKey(StringHook.DEBUG_UNTRACKED);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature,
      String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (debugUntracked && "<init>".equals(name)) {
      return new StringConstructorAdapter(mv, access, name, desc);
    } else {
      return mv;
    }
  }

  private static class StringConstructorAdapter extends AdviceAdapter {

    StringConstructorAdapter(MethodVisitor mv, int access, String name, String desc) {
      super(Opcodes.ASM9, mv, access, name, desc);
    }

    @Override
    protected void onMethodExit(int opcode) {
      if (opcode != ATHROW) {
        loadThis();
        invokeStatic(Type.getType(StringHook.class),
            Method.getMethod("void afterInit(java.lang.String)"));
      }
    }
  }
}
