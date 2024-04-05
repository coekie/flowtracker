package be.coekaerts.wouter.flowtracker.weaver;

import be.coekaerts.wouter.flowtracker.annotation.HookLocation;
import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import java.util.ArrayList;
import java.util.List;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class HookSpec {
  interface HookArgument {
    HookArgumentInstance applyTo(HookSpec hookSpec);
  }

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
      generator.dup();
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
          "be.coekaerts.wouter.flowtracker.tracker.Invocation preStart(String)"));
    }
  };

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
      super.visitMaxs(Math.max(maxStack, hookArguments.length + 1), maxLocals);
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
      };
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
