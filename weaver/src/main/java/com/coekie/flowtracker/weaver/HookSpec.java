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

import com.coekie.flowtracker.annotation.Hook;
import com.coekie.flowtracker.annotation.HookLocation;
import com.coekie.flowtracker.tracker.Invocation;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

/**
 * A HookSpec specifies the way we want to hook a particular method; that is adding a call to a
 * method in a _hook_ class at the start or end of it.
 * <p>
 * In practice, HookSpecs are created through annotations: every {@link Hook} annotation creates one
 * HookSpec instance.
 */
class HookSpec {
  /** Specifies an argument that should be passed into a hook method. */
  interface HookArgument {
    HookArgumentInstance applyTo(HookSpec hookSpec);
  }

  /**
   * A HookArgument applied to a specific method. A HookArgumentInstance can be stateful/mutable;
   * concretely the {@link #onMethodEnter(GeneratorAdapter)} may create a local variable that is
   * later used in {@link #load(GeneratorAdapter)}.
   */
  interface HookArgumentInstance {
    /**
     * Add code at the start of the method (optional).
     */
    default void onMethodEnter(GeneratorAdapter generator) {
    }

    /** Load the value of this argument on the stack */
    void load(GeneratorAdapter generator);

    Type getType(HookSpec hookSpec);
  }

  static final HookArgument THIS = spec -> new HookArgumentInstance() {
    @Override public void load(GeneratorAdapter generator) {
      generator.loadThis();
    }

    @Override public Type getType(HookSpec hookSpec) {
      return hookSpec.targetClass;
    }
  };

  /** Return value. Only works as first argument; assumes the return value is on top of the stack */
  static final HookArgument RETURN = spec -> new HookArgumentInstance() {
    @Override public void load(GeneratorAdapter generator) {
      if (spec.targetMethod.getReturnType().getSize() == 1) {
        generator.dup();
      } else {
        generator.dup2();
      }
    }

    @Override public Type getType(HookSpec hookSpec) {
      return spec.targetMethod.getReturnType();
    }
  };

  private static class ArgHookArgument implements HookArgumentInstance {
    private final int index;

    ArgHookArgument(int index) {
      this.index = index;
    }

    @Override
    public void load(GeneratorAdapter generator) {
      generator.loadArg(index);
    }

    @Override public Type getType(HookSpec hookSpec) {
      return hookSpec.cacheTargetMethodArgumentTypes[index];
    }
  }

  static HookArgument field(Type owner, String name, Type type) {
    return spec -> new HookArgumentInstance() {
      @Override
      public void load(GeneratorAdapter generator) {
        generator.loadThis();
        generator.getField(owner, name, type);
      }

      @Override
      public Type getType(HookSpec hookSpec) {
        return type;
      }
    };
  }

  /**
   * Argument for hook method that is calculated when entering the method, stored in a fresh local
   * variable, and loaded again when calling the hook.
   */
  abstract static class OnEnterHookArgumentInstance implements HookArgumentInstance {
    private final Type type;

    /** The index of the local variable to store the value in */
    private int localIndex = -1;

    OnEnterHookArgumentInstance(Type type) {
      this.type = type;
    }

    abstract void loadOnMethodEnter(GeneratorAdapter generator);

    @Override
    public void onMethodEnter(GeneratorAdapter generator) {
      if (localIndex != -1) {
        throw new IllegalStateException("Cannot reuse " + this);
      }
      localIndex = generator.newLocal(type);

      loadOnMethodEnter(generator);
      generator.storeLocal(localIndex);
    }

    @Override
    public void load(GeneratorAdapter generator) {
      if (localIndex == -1) {
        throw new IllegalStateException("onMethodEnter should have been called first");
      }
      generator.loadLocal(localIndex);
    }

    @Override
    public Type getType(HookSpec hookSpec) {
      return type;
    }
  }

  static final HookArgument ARG0 = spec -> new ArgHookArgument(0);
  static final HookArgument ARG1 = spec -> new ArgHookArgument(1);
  static final HookArgument ARG2 = spec -> new ArgHookArgument(2);
  static final HookArgument ARG3 = spec -> new ArgHookArgument(3);

  /** The {@link Invocation} of invoking the hooked method */
  static final HookArgument INVOCATION = spec -> new OnEnterHookArgumentInstance(
      Type.getType(Invocation.class)) {
    @Override
    void loadOnMethodEnter(GeneratorAdapter generator) {
      generator.push(
          Invocation.signature(spec.targetMethod.getName(), spec.targetMethod.getDescriptor()));
      generator.invokeStatic(Type.getType(Invocation.class), Method.getMethod(
          "com.coekie.flowtracker.tracker.Invocation preStart(String)"));
    }
  };

  /**
   * Does the actual instrumentation that the {@link HookSpec} describes: adds the call to the hook
   * method in the original method.
   */
  private class HookMethodAdapter extends AdviceAdapter {
    private final List<HookArgumentInstance> argumentInstances = createArgumentInstance();

    private HookMethodAdapter(MethodVisitor mv, int access, String name, String desc) {
      super(Opcodes.ASM9, mv, access, name, desc);
    }

    @Override
    protected void onMethodEnter() {
      for (HookArgumentInstance argumentInstance : argumentInstances) {
        argumentInstance.onMethodEnter(this);
      }
      if (location == HookLocation.ON_ENTER) {
        insertHook();
      }
      super.onMethodEnter();
    }

    @Override
    protected void onMethodExit(int opcode) {
      if (opcode != ATHROW && location == HookLocation.ON_RETURN) {
        insertHook();
      }
    }

    private void insertHook() {
      for (HookArgumentInstance argumentInstance : argumentInstances) {
        argumentInstance.load(this);
      }
      invokeStatic(hookClass, hookMethod);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
      int hookStackSize = 0;
      for (HookArgumentInstance argumentInstance : argumentInstances) {
        hookStackSize += argumentInstance.getType(HookSpec.this).getSize();
      }
      // in theory anything could be on the stack at the point where we call our hook; but in
      // practice it's either nothing or only the return value
      hookStackSize += HookSpec.this.targetMethod.getReturnType().getSize();

      super.visitMaxs(Math.max(maxStack, hookStackSize), maxLocals);
    }
  }

  private final Type targetClass;
  private final Method targetMethod;
  private final Type hookClass;
  private final HookLocation location;
  private final HookArgument[] hookArguments;
  private final Method hookMethod;
  private final Type[] cacheTargetMethodArgumentTypes;

  HookSpec(Type targetClass, Method targetMethod,
      Type hookClass, Method hookMethod, HookLocation location, HookArgument... hookArguments) {
    this.targetClass = targetClass;
    this.targetMethod = targetMethod;
    this.hookClass = hookClass;
    this.hookMethod = hookMethod;
    this.location = location;
    this.hookArguments = hookArguments;
    this.cacheTargetMethodArgumentTypes = targetMethod.getArgumentTypes();
  }

  MethodVisitor createMethodAdapter(MethodVisitor mv, int access, String name, String desc) {
    return new HookMethodAdapter(mv, access, name, desc);
  }

  private List<HookArgumentInstance> createArgumentInstance() {
    List<HookArgumentInstance> result = new ArrayList<>();
    for (var hookArgument : hookArguments) {
      result.add(hookArgument.applyTo(HookSpec.this));
    }
    return result;
  }

  /**
   * Check if the hook method can be called with the configured arguments
   * This is actually loading the classes, so we don't do this at runtime all the time, but only in
   * tests.
   */
  void typeCheck() {
    List<HookArgumentInstance> argumentInstances = createArgumentInstance();
    Type[] hookArgTypes = hookMethod.getArgumentTypes();
    if (hookArgTypes.length != argumentInstances.size()) {
      throw new IllegalStateException("Argument count mismatch on " + this);
    }
    for (int i = 0; i < argumentInstances.size(); i++) {
      Type givenType = argumentInstances.get(i).getType(this);
      if (givenType.getClassName().startsWith("org.springframework")) {
        continue; // TODO generic way to exclude some hook arguments from type checks
      }
      if (!canAssign(givenType, hookArgTypes[i])) {
        throw new IllegalStateException("Cannot assign " + givenType + " to " + hookArgTypes[i]
            + " for " + this + " arg " + i);
      }
    }
  }

  private boolean canAssign(Type givenType, Type requiredType) {
    if (requiredType.equals(givenType)) {
      return true;
    }
    if (requiredType.getSort() == Type.OBJECT && givenType.getSort() == Type.OBJECT) {
      try {
        // this is actually loading the classes. this is why we don't do this at runtime all the
        // time, but only in tests
        Class<?> requiredClass = Class.forName(requiredType.getClassName());
        Class<?> givenClass = Class.forName(givenType.getClassName());
        return requiredClass.isAssignableFrom(givenClass);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "HookSpec{" +
        "targetClass=" + targetClass +
        ", targetMethod=" + targetMethod +
        ", hookClass=" + hookClass +
        ", hookMethod=" + hookMethod +
        '}';
  }
}
