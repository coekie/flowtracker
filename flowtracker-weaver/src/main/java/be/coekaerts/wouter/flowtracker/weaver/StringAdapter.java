package be.coekaerts.wouter.flowtracker.weaver;

import be.coekaerts.wouter.flowtracker.hook.StringHook;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

/**
 * Instrumentation for {@link String}.
 *
 * Adds a hack to make it possible for {@link StringHook} to get the tracker based on the
 * String.value and String.offset.
 */
public class StringAdapter extends ClassVisitor {
  private final boolean debugUntracked;

  public StringAdapter(ClassVisitor cv, Map<String, String> config) {
    super(Opcodes.ASM9, cv);
    debugUntracked = config.containsKey(StringHook.DEBUG_UNTRACKED);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature,
      String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (debugUntracked && "<init>".equals(name)) {
      return new StringConstructorAdapter(mv, access, name, desc);
    } else {
      return mv;
    }
  }

  private static class StringConstructorAdapter extends AdviceAdapter {

    public StringConstructorAdapter(MethodVisitor mv, int access, String name, String desc) {
      super(Opcodes.ASM9, mv, access, name, desc);
    }

    @Override
    protected void onMethodExit(int opcode) {
      if (opcode != ATHROW) {
        loadThis();
        invokeStatic(Type.getType(StringHook.class),
            Method.getMethod("void afterInit(java.lang.String)"));
      }
    }
  }
}
