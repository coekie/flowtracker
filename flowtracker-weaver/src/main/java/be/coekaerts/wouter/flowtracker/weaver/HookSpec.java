package be.coekaerts.wouter.flowtracker.weaver;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class HookSpec {
  public static abstract class HookArgument {
    /**
     * Add code at the start of the method (optional).
     */
    void onMethodEnter(GeneratorAdapter generator) {
    }

    /** Load the value of this argument on the stack */
    abstract void load(GeneratorAdapter generator);

    abstract Type getType(HookSpec hookSpec);
  }

  public static final HookArgument THIS = new HookArgument() {
    @Override void load(GeneratorAdapter generator) {
      generator.loadThis();
    }

    @Override Type getType(HookSpec hookSpec) {
      return hookSpec.targetClass;
    }
  };

  private static class ArgHookArgument extends HookArgument {
    private final int index;

    public ArgHookArgument(int index) {
      this.index = index;
    }

    @Override
    public void load(GeneratorAdapter generator) {
      generator.loadArg(index);
    }

    @Override Type getType(HookSpec hookSpec) {
      return hookSpec.cacheTargetMethodArgumentTypes[index];
    }
  }

  public static HookArgument field(Type owner, String name, Type type) {
    return new HookArgument() {
      @Override
      void load(GeneratorAdapter generator) {
        generator.loadThis();
        generator.getField(owner, name, type);
      }

      @Override
      Type getType(HookSpec hookSpec) {
        return type;
      }
    };
  }

  /**
   * Argument for hook method that is calculated when entering the method, stored in a fresh local
   * variable, and loaded again when calling the hook.
   */
  abstract static class OnEnterHookArgument extends HookArgument {
    private final Type type;

    /**
     * The index of the local variable to store the value in. This depends on if the method is
     * static or not (first slot taken by "this") and the arguments.
     * (Alternatively we could let onMethodEnter determine this, but then it needs to be passed back
     * into "load".)
     */
    private final int localIndex;

    OnEnterHookArgument(Type type, int localIndex) {
      this.type = type;
      this.localIndex = localIndex;
    }

    abstract void loadOnMethodEnter(GeneratorAdapter generator);

    @Override
    void onMethodEnter(GeneratorAdapter generator) {
      int local = generator.newLocal(Type.getType(String.class));
      if (local != localIndex) {
        throw new IllegalStateException("Expected localIndex " + localIndex + " but got " + local);
      }

      loadOnMethodEnter(generator);
      generator.storeLocal(localIndex);
    }

    @Override
    void load(GeneratorAdapter generator) {
      generator.loadLocal(localIndex);
    }

    @Override
    Type getType(HookSpec hookSpec) {
      return type;
    }
  }

  public static final HookArgument ARG0 = new ArgHookArgument(0);
  public static final HookArgument ARG1 = new ArgHookArgument(1);
  public static final HookArgument ARG2 = new ArgHookArgument(2);
  public static final HookArgument ARG3 = new ArgHookArgument(3);

  private class HookMethodAdapter extends AdviceAdapter {
    private final boolean hasReturnType;

    private HookMethodAdapter(MethodVisitor mv, int access, String name, String desc) {
      super(Opcodes.ASM9, mv, access, name, desc);
      hasReturnType = desc.charAt(desc.indexOf(')') + 1) != 'V';
    }

    @Override
    protected void onMethodEnter() {
      for (HookArgument argument : hookArguments) {
        argument.onMethodEnter(this);
      }
      super.onMethodEnter();
    }

    @Override
    protected void onMethodExit(int opcode) {
      if (opcode != ATHROW) {
        if (hasReturnType) {
          dup(); // copy result; one for the hook, one to return
        }
        for (HookArgument argument : hookArguments) {
          argument.load(this);
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
  private final Class<?> hookClass;
  private final HookArgument[] hookArguments;
  private final Method hookMethod;
  private final Type[] cacheTargetMethodArgumentTypes;

  public HookSpec(Type targetClass, Method targetMethod,
      Class<?> hookClass, Method hookMethod, HookArgument... hookArguments) {
    super();
    this.targetClass = targetClass;
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
