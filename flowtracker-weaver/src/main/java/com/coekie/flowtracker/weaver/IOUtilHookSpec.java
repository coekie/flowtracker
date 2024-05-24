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

import com.coekie.flowtracker.weaver.HookSpec.HookArgument;
import com.coekie.flowtracker.weaver.HookSpec.OnEnterHookArgumentInstance;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

/** Special arguments for {@link com.coekie.flowtracker.hook.IOUtilHook} */
public class IOUtilHookSpec {
  /** Positions of ByteBuffers passed into a method as an array, on method enter */
  @SuppressWarnings("unused") // used in IOUtilHook
  static final HookArgument BUFFER_POSITIONS = spec -> new OnEnterHookArgumentInstance(
      Type.getType(int[].class)) {
    @Override
    void loadOnMethodEnter(GeneratorAdapter generator) {
      generator.loadArg(1);
      generator.invokeStatic(Type.getObjectType("com/coekie/flowtracker/hook/IOUtilHook"),
          Method.getMethod("int[] positions(java.nio.ByteBuffer[])"));
    }
  };
}
