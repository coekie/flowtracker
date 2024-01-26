package be.coekaerts.wouter.flowtracker.weaver;

import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.RETURN;
import static be.coekaerts.wouter.flowtracker.weaver.HookSpec.THIS;

import be.coekaerts.wouter.flowtracker.weaver.HookSpec.HookArgument;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

class HookSpecTransformer implements Transformer {

  private final Map<String, ClassHookSpec> specs = new HashMap<>();

  void register(String targetClass, String targetMethodName, String targetMethodDesc,
      String hookMethodClass, String hookMethodName, String hookMethodDesc, HookArgument... args) {
    ClassHookSpec spec = specs.get(targetClass);
    if (spec == null) {
      spec = new ClassHookSpec(Type.getObjectType(targetClass));
      specs.put(targetClass, spec);
    }
    spec.addMethodHookSpec(new Method(targetMethodName, targetMethodDesc),
        Type.getObjectType(hookMethodClass), new Method(hookMethodName, hookMethodDesc), args);
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
        Type.getObjectType("be/coekaerts/wouter/flowtracker/hook/URLConnectionHook"),
        new Method("afterGetInputStream", "(Ljava/io/InputStream;Ljava/net/URLConnection;)V"),
        RETURN, THIS);
    return spec;
  }

  @Override
  public ClassVisitor transform(String className, ClassVisitor cv) {
    ClassHookSpec spec = getSpec(className);
    return spec == null ? cv : spec.transform(className, cv);
  }
}
