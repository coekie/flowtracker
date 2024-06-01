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

import com.coekie.flowtracker.annotation.HookLocation;
import com.coekie.flowtracker.util.Logger;
import com.coekie.flowtracker.weaver.HookSpec.HookArgument;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

class ClassHookSpec implements Transformer {
  private static final Logger logger = new Logger("ClassHookSpec");

  private class HookClassAdapter extends ClassVisitor {
    private HookClassAdapter(ClassVisitor cv) {
      super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
        String[] exceptions) {
      MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

      Method method = new Method(name, desc);
      HookSpec hookSpec = methodHookSpecs.get(method);
      if (hookSpec != null) {
        logger.info("Transforming %s.%s%s", targetClass.getClassName(), name, desc);
        return hookSpec.createMethodAdapter(mv, access, name, desc);
      } else {
        return mv;
      }
    }
  }

  private final Type targetClass;
  private final Map<Method, HookSpec> methodHookSpecs = new HashMap<>();

  ClassHookSpec(Type targetClass) {
    this.targetClass = targetClass;
  }

  ClassHookSpec addMethodHookSpec(Method targetMethod, Type hookClass, Method hookMethod,
      HookLocation location, HookArgument... hookArguments) {
    HookSpec hookSpec =
        new HookSpec(targetClass, targetMethod, hookClass, hookMethod, location, hookArguments);
    methodHookSpecs.put(targetMethod, hookSpec);
    return this;
  }

  @Override
  public ClassVisitor transform(ClassLoader classLoader, String className, ClassVisitor cv) {
    return new HookClassAdapter(cv);
  }

  Type getTargetClass() {
    return targetClass;
  }

  void typeCheck() {
    for (HookSpec hookSpec : methodHookSpecs.values()) {
      hookSpec.typeCheck();
    }
  }
}
