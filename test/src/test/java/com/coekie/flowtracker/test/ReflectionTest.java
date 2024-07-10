package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.test.TrackTestHelper.getClassOriginTrackerContent;
import static com.coekie.flowtracker.test.TrackTestHelper.stringTracker;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.TrackerSnapshot;
import org.junit.Test;

public class ReflectionTest {
  @Test
  public void getClassName() {
    String str = ClassTest.class.getName();
    TrackerSnapshot snapshot = TrackerSnapshot.of(stringTracker(str));
    assertThat(snapshot.getParts()).hasSize(1);
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(0)))
        .isEqualTo("com.coekie.flowtracker.test.ClassTest");
  }

  @Test
  public void getClassName_array() {
    String str = ClassTest[].class.getName();
    assertThat(stringTracker(str)).isNull();
  }

  @Test
  public void testFieldName() {
    class Foo {
      @SuppressWarnings("unused")
      int myField;
    }
    String str = Foo.class.getDeclaredFields()[0].getName();
    TrackerSnapshot snapshot = TrackerSnapshot.of(stringTracker(str));
    assertThat(snapshot.getParts()).hasSize(1);
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(0)))
        .isEqualTo("myField");
  }

  @Test
  public void testMethodName() {
    class Foo {
      @SuppressWarnings("unused")
      void myMethod() {

      }
    }
    String str = Foo.class.getDeclaredMethods()[0].getName();
    TrackerSnapshot snapshot = TrackerSnapshot.of(stringTracker(str));
    assertThat(snapshot.getParts()).hasSize(1);
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(0)))
        .isEqualTo("myMethod");
  }
}
