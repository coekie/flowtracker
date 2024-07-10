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

import com.coekie.flowtracker.tracker.Invocation;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.Method;

/**
 * Suspends a pending {@link Invocation} on class loading and initialization.
 *
 * @see Invocation#suspend()
 */
class SuspendInvocationTransformer implements Transformer {
  private final boolean applyToAllForTesting;

  SuspendInvocationTransformer() {
    this(false);
  }

  SuspendInvocationTransformer(boolean applyToAllForTesting) {
    this.applyToAllForTesting = applyToAllForTesting;
  }

  @Override
  public ClassVisitor transform(ClassLoader classLoader, String className, ClassVisitor cv) {
    return new SuspendInvocationClassVisitor(className, cv);
  }

  private class SuspendInvocationClassVisitor extends ClassVisitor {
    private final String className;

    private SuspendInvocationClassVisitor(String className, ClassVisitor cv) {
      super(Opcodes.ASM9, cv);
      this.className = className;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
        String[] exceptions) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

      boolean isLoadClass = className.equals("java/lang/ClassLoader")
          && name.equals("loadClass")
          && desc.equals("(Ljava/lang/String;)Ljava/lang/Class;");
      if (isLoadClass || name.equals("<clinit>") || applyToAllForTesting) {
        return new SuspendInvocationMethodAdapter(mv, access, name, desc);
      } else {
        return mv;
      }
    }
  }

  private static class SuspendInvocationMethodAdapter extends AdviceAdapter {
    /** The index of the local variable to store the Invocation in */
    private int localIndex = -1;

    SuspendInvocationMethodAdapter(
        MethodVisitor mv, int access, String name, String desc) {
      super(Opcodes.ASM9, mv, access, name, desc);
    }

    @Override
    protected void onMethodEnter() {
      localIndex = newLocal(Types.INVOCATION);
      invokeStatic(Types.INVOCATION, Method.getMethod(
          "com.coekie.flowtracker.tracker.Invocation suspend()"));
      storeLocal(localIndex);
      super.onMethodEnter();
    }

    @Override
    protected void onMethodExit(int opcode) {
      if (opcode != ATHROW) {
        loadLocal(localIndex);
        invokeStatic(Types.INVOCATION,
            Method.getMethod("void unsuspend(com.coekie.flowtracker.tracker.Invocation)"));
      }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
      super.visitMaxs(Math.max(maxStack, 1), maxLocals);
    }
  }
}
