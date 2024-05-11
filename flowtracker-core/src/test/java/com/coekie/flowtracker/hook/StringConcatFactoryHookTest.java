package com.coekie.flowtracker.hook;

import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.TrackerPoint;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.junit.Test;

// some basic tests for StringConcatFactoryHook.
// this is tested more thoroughly, end-to-end, in StringTest.
public class StringConcatFactoryHookTest {
  @Test
  public void testTrivial() throws Throwable {
    test(MethodType.methodType(String.class), "foo", "",
        mh -> (String) mh.invokeExact(), "foo");
  }

  @Test
  public void testCharLiterals() throws Throwable {
    test(MethodType.methodType(String.class, String.class, char.class, char.class, char.class,
            char.class, TrackerPoint.class, TrackerPoint.class, TrackerPoint.class),
        "(\1,\1,\1,\1,\1)", ".T.TT",
        mh -> (String) mh.invokeExact("one", '2', '3', '4', '5',
            (TrackerPoint) null, (TrackerPoint) null, (TrackerPoint) null),
        "(one,2,3,4,5)");
  }

  private void test(MethodType methodType, String recipe, String mask, Invoke invoke,
      String expected) throws Throwable {
    CallSite callSite = StringConcatFactoryHook.makeConcatWithConstants(MethodHandles.lookup(),
        "myName", methodType, recipe, mask);
    String result = (String) invoke.apply(callSite.getTarget());
    assertThat(result).isEqualTo(expected);
  }

  interface Invoke {
    Object apply(MethodHandle mh) throws Throwable;
  }
}