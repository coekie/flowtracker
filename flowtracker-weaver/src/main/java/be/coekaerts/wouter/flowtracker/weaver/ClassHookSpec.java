package be.coekaerts.wouter.flowtracker.weaver;

import be.coekaerts.wouter.flowtracker.util.Logger;
import be.coekaerts.wouter.flowtracker.weaver.HookSpec.HookArgument;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

class ClassHookSpec implements ClassAdapterFactory {
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
  private final Class<?> hookClass;

  private final Map<Method, HookSpec> methodHookSpecs = new HashMap<>();

  ClassHookSpec(Type targetClass, Class<?> hookClass) {
    this.targetClass = targetClass;
    this.hookClass = hookClass;
  }

  ClassHookSpec addMethodHookSpec(String targetMethod, String hookMethod,
      HookArgument... hookArguments) {
    return addMethodHookSpec(Method.getMethod(targetMethod), Method.getMethod(hookMethod), hookArguments);
  }

  ClassHookSpec addMethodHookSpec(Method targetMethod, Method hookMethod,
      HookArgument... hookArguments) {
    HookSpec hookSpec =
        new HookSpec(targetClass, targetMethod, hookClass, hookMethod, hookArguments);
    methodHookSpecs.put(targetMethod, hookSpec);
    return this;
  }

  public ClassVisitor createClassAdapter(String className, ClassVisitor cv) {
    return new HookClassAdapter(cv);
  }

  Type getTargetClass() {
    return targetClass;
  }
}
