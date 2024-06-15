package com.coekie.flowtracker.weaver;

import static com.coekie.flowtracker.tracker.Context.context;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.annotation.HookLocation;
import com.coekie.flowtracker.tracker.FixedOriginTracker;
import com.coekie.flowtracker.tracker.Invocation;
import com.coekie.flowtracker.tracker.TrackerPoint;
import com.coekie.flowtracker.weaver.HookSpec.HookArgument;
import com.coekie.flowtracker.weaver.HookSpec.OnEnterHookArgumentInstance;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class HookSpecTest {
  public static StringBuilder log;

  @Before
  public void before() {
    log = new StringBuilder();
  }

  @Test
  public void test() throws ReflectiveOperationException {
    ClassHookSpec classHookSpec = new ClassHookSpec(Type.getType(Foo.class));
    transformAndRun(classHookSpec.addMethodHookSpec(Method.getMethod("void bar(int)"),
        Type.getType(MyHook.class), Method.getMethod("void afterBar(java.lang.Object,int)"),
        HookLocation.ON_RETURN,
        HookSpec.THIS, HookSpec.ARG0));
    assertThat(log.toString()).isEqualTo(
        "bar 5\n"
            + "afterBar Foo2 5\n");
  }

  @Test
  public void testReturnValue() throws ReflectiveOperationException {
    ClassHookSpec classHookSpec = new ClassHookSpec(Type.getType(WithReturnValue.class));
    transformAndRun(classHookSpec.addMethodHookSpec(Method.getMethod("String withReturnValue(int)"),
        Type.getType(MyHook.class),
        Method.getMethod("void afterWithReturnValue(java.lang.String,int)"),
        HookLocation.ON_RETURN,
        HookSpec.RETURN, HookSpec.ARG0));
    assertThat(log.toString()).isEqualTo(
        "withReturnValue 5\n"
            + "afterWithReturnValue retVal 5\n");
  }

  @Test
  public void testOnEnterArgument() throws ReflectiveOperationException {
    HookArgument arg0OnEnter = spec -> new OnEnterHookArgumentInstance(Type.getType(String.class)) {
      @Override
      void loadOnMethodEnter(GeneratorAdapter generator) {
        generator.loadArg(0);
      }
    };

    ClassHookSpec classHookSpec = new ClassHookSpec(Type.getType(HookedWithOnEnter.class));
    transformAndRun(
        classHookSpec.addMethodHookSpec(Method.getMethod("void withOnEnter(java.lang.String)"),
            Type.getType(MyHook.class),
            Method.getMethod("void afterWithOnEnter(java.lang.String,java.lang.String)"),
            HookLocation.ON_RETURN,
            arg0OnEnter, HookSpec.ARG0));
    assertThat(log.toString()).isEqualTo(
        "withOnEnter originalArg 2\n"
            + "afterWithOnEnter originalArg updatedArg\n");
  }

  @Test
  public void testInvocation() throws ReflectiveOperationException {
    Invocation.createCalling(context(), "withInvocation ()V")
        .setArg(0, TrackerPoint.of(new FixedOriginTracker(1000), 777));
    ClassHookSpec classHookSpec = new ClassHookSpec(Type.getType(HookedWithInvocation.class));
    transformAndRun(classHookSpec.addMethodHookSpec(Method.getMethod("void withInvocation()"),
        Type.getType(MyHook.class), Method.getMethod(
            "void afterWithInvocation(com.coekie.flowtracker.tracker.Invocation)"),
        HookLocation.ON_RETURN,
        HookSpec.INVOCATION));
    assertThat(log.toString()).isEqualTo(
        "withInvocation\n"
            + "afterWithInvocation 777\n");
  }

  @Test
  public void testField() throws ReflectiveOperationException {
    ClassHookSpec classHookSpec = new ClassHookSpec(Type.getType(WithField.class));
    HookArgument[] hookArguments = new HookArgument[]{HookSpec.field(Type.getType("Lcom/coekie/flowtracker/weaver/WithField2;"),
        "i", Type.getType(int.class))};
    transformAndRun(classHookSpec.addMethodHookSpec(Method.getMethod("void withField()"),
        Type.getType(MyHook.class), Method.getMethod("void afterWithField(int)"),
        HookLocation.ON_RETURN, hookArguments));
    assertThat(log.toString()).isEqualTo("afterWithField 10\n");
  }

  @Test
  public void testOnEnter() throws ReflectiveOperationException {
    ClassHookSpec classHookSpec = new ClassHookSpec(Type.getType(Foo.class));
    transformAndRun(classHookSpec.addMethodHookSpec(Method.getMethod("void bar(int)"),
        Type.getType(MyHook.class), Method.getMethod("void before(java.lang.Object,int)"),
        HookLocation.ON_ENTER, HookSpec.THIS, HookSpec.ARG0));
    assertThat(log.toString()).isEqualTo(
        "before Foo2 5\n"
            + "bar 5\n");
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
      assertThat(Invocation.peekPending()).isNull();
      // to match its only use-case, unsuspend (restore) the invocation in the hook
      Invocation.unsuspend(invocation);
    }

    public static void afterWithField(int i) {
      log("afterWithField", i);
    }

    public static void before(Object thiz, int i) {
      log("before", thiz.getClass().getSimpleName(), i);
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
    Invocation.createCalling(context(), "something else");
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