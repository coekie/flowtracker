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
import com.coekie.flowtracker.weaver.HookSpec.HookArgument;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

class HookSpecTransformer implements Transformer {

  private final Map<String, ClassHookSpec> specs = new HashMap<>();

  void register(String targetClass, String targetMethodName, String targetMethodDesc,
      String hookMethodClass, String hookMethodName, String hookMethodDesc, HookLocation location,
      HookArgument... args) {
    ClassHookSpec spec = specs.get(targetClass);
    if (spec == null) {
      spec = new ClassHookSpec(Type.getObjectType(targetClass));
      specs.put(targetClass, spec);
    }
    spec.addMethodHookSpec(new Method(targetMethodName, targetMethodDesc),
        Type.getObjectType(hookMethodClass), new Method(hookMethodName, hookMethodDesc), location,
        args);
  }

  private ClassHookSpec getSpec(String className) {
    if (className.endsWith("URLConnection")) {
      return urlConnectionHook(className);
    }
    return specs.get(className);
  }

  // untested
  private ClassHookSpec urlConnectionHook(String urlConnectionSubclass) {
    ClassHookSpec spec = new ClassHookSpec(
        Type.getObjectType(urlConnectionSubclass.replace('.', '/')));
    spec.addMethodHookSpec(new Method("getInputStream", "()Ljava.io.InputStream;"),
        Type.getObjectType("com/coekie/flowtracker/hook/URLConnectionHook"),
        new Method("afterGetInputStream", "(Ljava/io/InputStream;Ljava/net/URLConnection;)V"),
        HookLocation.ON_RETURN,
        HookSpec.RETURN, HookSpec.THIS);
    return spec;
  }

  @Override
  public ClassVisitor transform(ClassLoader classLoader, String className, ClassVisitor cv) {
    ClassHookSpec spec = getSpec(className);
    return spec == null ? cv : spec.transform(classLoader, className, cv);
  }

  void typeCheck() {
    for (ClassHookSpec classHookSpec : specs.values()) {
      classHookSpec.typeCheck();
    }
  }
}
