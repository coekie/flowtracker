package be.coekaerts.wouter.flowtracker.weaver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Invocation;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import be.coekaerts.wouter.flowtracker.weaver.HookSpec.HookArgument;
import be.coekaerts.wouter.flowtracker.weaver.HookSpec.OnEnterHookArgumentInstance;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

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
    HookArgument arg0OnEnter = spec -> new OnEnterHookArgumentInstance(Type.getType(String.class)) {
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
  public void testInvocation() throws ReflectiveOperationException {
    Invocation.calling("withInvocation ()V")
        .setArg(0, TrackerPoint.of(new FixedOriginTracker(1000), 777));
    transformAndRun(new ClassHookSpec(Type.getType(HookedWithInvocation.class), MyHook.class)
        .addMethodHookSpec("void withInvocation()",
            "void afterWithInvocation(be.coekaerts.wouter.flowtracker.tracker.Invocation)",
            HookSpec.INVOCATION));
    assertEquals("withInvocation\n"
        + "afterWithInvocation 777\n", log.toString());
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
    TransformerTestUtils.transformAndRun(classHookSpec, classHookSpec.getTargetClass(),
        constructArgs);
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

    public static void afterWithInvocation(Invocation invocation) {
      log("afterWithInvocation", Invocation.getArgPoint(invocation, 0).index);
    }

    public static void afterWithSuspendedInvocation(Invocation invocation) {
      log("afterWithSuspendedInvocation", Invocation.getArgPoint(invocation, 0).index);
      assertNull(Invocation.peekPending());
      // to match its only use-case, unsuspend (restore) the invocation in the hook
      Invocation.unsuspend(invocation);
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

class HookedWithInvocation implements Runnable {
  @Override
  public void run() {
    withInvocation();
  }

  void withInvocation() {
    Invocation.calling("something else");
    HookSpecTest.log("withInvocation");
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