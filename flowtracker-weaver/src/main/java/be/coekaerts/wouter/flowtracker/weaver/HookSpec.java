package be.coekaerts.wouter.flowtracker.weaver;

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
  public interface HookArgument {
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

  public static final HookArgument THIS = spec -> new HookArgumentInstance() {
    @Override public void load(GeneratorAdapter generator) {
      generator.loadThis();
    }

    @Override public Type getType(HookSpec hookSpec) {
      return hookSpec.targetClass;
    }
  };

  private static class ArgHookArgument implements HookArgumentInstance {
    private final int index;

    public ArgHookArgument(int index) {
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

  public static HookArgument field(Type owner, String name, Type type) {
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

  public static final HookArgument ARG0 = spec -> new ArgHookArgument(0);
  public static final HookArgument ARG1 = spec -> new ArgHookArgument(1);
  public static final HookArgument ARG2 = spec -> new ArgHookArgument(2);
  public static final HookArgument ARG3 = spec -> new ArgHookArgument(3);

  /** The {@link Invocation} of invoking the hooked method */
  public static final HookArgument INVOCATION = spec -> new OnEnterHookArgumentInstance(
      Type.getType(Invocation.class)) {
    @Override
    void loadOnMethodEnter(GeneratorAdapter generator) {
      generator.push(
          Invocation.signature(spec.targetMethod.getName(), spec.targetMethod.getDescriptor()));
      generator.invokeStatic(Type.getType(Invocation.class), Method.getMethod(
          "be.coekaerts.wouter.flowtracker.tracker.Invocation start(String)"));
    }
  };

  /**
   * The {@link Invocation} that was active when this method was invoked.
   * Having this is an argument to a hook causes the invocation to get suspended.
   * @see Invocation#suspend()
   */
  public static final HookArgument SUSPENDED_INVOCATION = spec -> new OnEnterHookArgumentInstance(
      Type.getType(Invocation.class)) {
    @Override
    void loadOnMethodEnter(GeneratorAdapter generator) {
      generator.invokeStatic(Type.getType(Invocation.class), Method.getMethod(
          "be.coekaerts.wouter.flowtracker.tracker.Invocation suspend()"));
    }
  };

  private class HookMethodAdapter extends AdviceAdapter {
    private final boolean hasReturnType;
    private final List<HookArgumentInstance> argumentInstances;

    private HookMethodAdapter(MethodVisitor mv, int access, String name, String desc) {
      super(Opcodes.ASM9, mv, access, name, desc);
      hasReturnType = desc.charAt(desc.indexOf(')') + 1) != 'V';
      argumentInstances = new ArrayList<>();
      for (var hookArgument : hookArguments) {
        argumentInstances.add(hookArgument.applyTo(HookSpec.this));
      }
    }

    @Override
    protected void onMethodEnter() {
      for (HookArgumentInstance argumentInstance : argumentInstances) {
        argumentInstance.onMethodEnter(this);
      }
      super.onMethodEnter();
    }

    @Override
    protected void onMethodExit(int opcode) {
      if (opcode != ATHROW) {
        if (hasReturnType) {
          dup(); // copy result; one for the hook, one to return
        }
        for (HookArgumentInstance argumentInstance : argumentInstances) {
          argumentInstance.load(this);
        }
        invokeStatic(Type.getType(hookClass), getHookMethod());
      }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
      // pessimistic upper limit: we push hookArguments on the stack + optionally dup return value
      super.visitMaxs(maxStack + hookArguments.length + (hasReturnType ? 1 : 0), maxLocals);
    }
  }

  private final Type targetClass;
  private final Method targetMethod;
  private final Class<?> hookClass;
  private final HookArgument[] hookArguments;
  private final Method hookMethod;
  private final Type[] cacheTargetMethodArgumentTypes;

  public HookSpec(Type targetClass, Method targetMethod,
      Class<?> hookClass, Method hookMethod, HookArgument... hookArguments) {
    super();
    this.targetClass = targetClass;
    this.targetMethod = targetMethod;
    this.hookClass = hookClass;
    this.hookMethod = hookMethod;
    this.hookArguments = hookArguments;
    this.cacheTargetMethodArgumentTypes = targetMethod.getArgumentTypes();
  }

  private Method getHookMethod() {
    return hookMethod;
  }

  public MethodVisitor createMethodAdapter(MethodVisitor mv, int access, String name, String desc) {
    return new HookMethodAdapter(mv, access, name, desc);
  }
}
