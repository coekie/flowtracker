package be.coekaerts.wouter.flowtracker.weaver;

import static org.junit.Assert.assertEquals;

import be.coekaerts.wouter.flowtracker.weaver.HookSpec.HookArgument;
import be.coekaerts.wouter.flowtracker.weaver.HookSpec.OnEnterHookArgument;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

public class HookSpecTest {
  public static StringBuilder log;

  @Before
  public void before() {
    log = new StringBuilder();
  }

  @Test
  public void test() throws ReflectiveOperationException {
    transformAndRun(new ClassHookSpec(Type.getType(Foo.class), MyHook.class)
        .addMethodHookSpec("void bar(int)",
        "void afterBar(java.lang.Object,int)", HookSpec.THIS, HookSpec.ARG0));
    assertEquals("bar 5\n"
        + "afterBar Foo2 5\n", log.toString());
  }

  @Test
  public void testReturnValue() throws ReflectiveOperationException {
    transformAndRun(new ClassHookSpec(Type.getType(WithReturnValue.class), MyHook.class)
        .addMethodHookSpec("String withReturnValue(int)",
            "void afterWithReturnValue(java.lang.String,int)", HookSpec.ARG0));
    assertEquals("withReturnValue 5\n"
        + "afterWithReturnValue retVal 5\n", log.toString());
  }

  @Test
  public void testOnEnter() throws ReflectiveOperationException {
    HookArgument arg0OnEnter = new OnEnterHookArgument(Type.getType(String.class), 2) {
      @Override
      void loadOnMethodEnter(GeneratorAdapter generator) {
        generator.loadArg(0);
      }
    };

    transformAndRun(new ClassHookSpec(Type.getType(HookedWithOnEnter.class), MyHook.class)
        .addMethodHookSpec("void withOnEnter(java.lang.String)",
            "void afterWithOnEnter(java.lang.String,java.lang.String)",
            arg0OnEnter, HookSpec.ARG0));
    assertEquals("withOnEnter originalArg 2\n"
        + "afterWithOnEnter originalArg updatedArg\n", log.toString());
  }

  @Test
  public void testField() throws ReflectiveOperationException {
    transformAndRun(new ClassHookSpec(Type.getType(WithField.class), MyHook.class)
        .addMethodHookSpec("void withField()",
            "void afterWithField(int)",
            HookSpec.field(Type.getType("Lbe/coekaerts/wouter/flowtracker/weaver/WithField2;"),
                "i", Type.getType(int.class))));
    assertEquals("afterWithField 10\n", log.toString());
  }

  /**
   * Transform the class according to the spec, invoke its constructor and {@link Runnable#run()}
   */
  private void transformAndRun(ClassHookSpec classHookSpec, Object... constructArgs)
      throws ReflectiveOperationException {
    Class<?> clazz = transformClass(classHookSpec);
    Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
    constructor.setAccessible(true);
    Object instance = constructor.newInstance(constructArgs);
    ((Runnable) instance).run();
  }

  private static Class<?> transformClass(ClassHookSpec classHookSpec) {
    String className = classHookSpec.getTargetClass().getClassName();
    String suffix = "2"; // transformed class named className + suffix

    ClassWriter classWriter = new ClassWriter(0);
    StringWriter verifyStringWriter = new StringWriter();
    PrintWriter verifyPrintWriter = new PrintWriter(verifyStringWriter);
    StringWriter afterStringWriter = new StringWriter();
    PrintWriter afterPrintWriter = new PrintWriter(afterStringWriter);
    StringWriter beforeStringWriter = new StringWriter();
    PrintWriter beforePrintWriter = new PrintWriter(beforeStringWriter);

    // writes out bytecode to text after transformation
    TraceClassVisitor afterVisitor =
        new TraceClassVisitor(new CheckClassAdapter(classWriter), afterPrintWriter);
    ClassVisitor transformingVisitor =
        classHookSpec.createClassAdapter(afterVisitor);
    // writes out original bytecode to text
    TraceClassVisitor beforeVisitor =
        new TraceClassVisitor(transformingVisitor, beforePrintWriter);
    ClassRemapper remapper = new ClassRemapper(beforeVisitor,
        new SimpleRemapper(Map.of(classHookSpec.getTargetClass().getInternalName(),
            classHookSpec.getTargetClass().getInternalName() + suffix)));

    try {
      new ClassReader(className)
          .accept(remapper, ClassReader.EXPAND_FRAMES | ClassReader.SKIP_DEBUG);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // verify bytecode using asm. this is not as thorough as the jvm, but gives more helpful error
    // messages when it fails
    CheckClassAdapter.verify(new ClassReader(classWriter.toByteArray()), false, verifyPrintWriter);
    assertEquals("", verifyStringWriter.toString());

    try {
      return loadClass(className + suffix, classWriter.toByteArray());
    } catch (VerifyError e) {
      throw new AssertionError("Verification failed. Original code:\n" + beforeStringWriter
          + "\nTransformed code:\n" + afterStringWriter, e);
    }
  }

  /**
   * Load a class in a child classloader.
   */
  private static Class<?> loadClass(String className, byte[] byteCode) {
    ClassLoader classLoader = new ClassLoader(HookSpecTest.class.getClassLoader()) {
      @Override
      protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.equals(className)) {
          return defineClass(name, byteCode, 0, byteCode.length);
        }
        return super.findClass(name);
      }
    };
    try {
      return classLoader.loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static void log(Object... strs) {
    log.append(Stream.of(strs).map(Object::toString).collect(Collectors.joining(" ", "", "\n")));
  }

  @SuppressWarnings("UnusedDeclaration") // used by instrumented code
  public static class MyHook {
    public static void afterBar(Object thiz, int i) {
      log("afterBar", thiz.getClass().getSimpleName(), i);
    }

    public static void afterWithReturnValue(String returnValue, int i) {
      log("afterWithReturnValue", returnValue, i);
    }

    public static void afterWithOnEnter(String argOnEnter, String arg) {
      log("afterWithOnEnter", argOnEnter, arg);
    }

    public static void afterWithField(int i) {
      log("afterWithField", i);
    }
  }
}

// cannot be an inner class because we create a copy of it, and otherwise would get
// "IncompatibleClassChangeError: ...HookSpecTest and ...HookSpecTest$Foo2 disagree on InnerClasses
// attribute"
class Foo implements Runnable {
  @Override
  public void run() {
    bar(5);
  }

  @SuppressWarnings("SameParameterValue")
  void bar(int i) {
    HookSpecTest.log("bar", i);
  }
}

class WithReturnValue implements Runnable {
  @Override
  public void run() {
    withReturnValue(5);
  }

  @SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
  String withReturnValue(int i) {
    HookSpecTest.log("withReturnValue", i);
    return "retVal";
  }
}

class HookedWithOnEnter implements Runnable {
  @Override
  public void run() {
    withOnEnter("originalArg");
  }

  @SuppressWarnings({"SameParameterValue", "UnusedAssignment"})
  void withOnEnter(String arg) {
    int i = 1; // a local variable just to test that local vars get renumbered correctly
    i++;
    HookSpecTest.log("withOnEnter", arg, i);
    arg = "updatedArg";
  }
}

class WithField implements Runnable {
  int i;

  @Override
  public void run() {
    withField();
  }

  void withField() {
    i = 10;
  }
}