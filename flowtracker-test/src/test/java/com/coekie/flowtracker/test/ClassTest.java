package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.hook.StringHook.getStringTracker;
import static com.coekie.flowtracker.test.StringTest.getClassOriginTrackerContent;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.hook.InputStreamHook;
import com.coekie.flowtracker.tracker.TrackerSnapshot;
import java.io.InputStream;
import org.junit.Test;

public class ClassTest {
  @Test
  public void getResourceAsStream() {
    InputStream stream = URLTest.class.getResourceAsStream("ClassTest.class");
    TrackTestHelper.assertThatTrackerNode(InputStreamHook.getInputStreamTracker(stream))
        .hasPathStartingWith("Files")
        .hasPathEndingWith("ClassTest.class");
  }

  @Test
  public void getName() {
    String str = ClassTest.class.getName();
    TrackerSnapshot snapshot = TrackerSnapshot.of(getStringTracker(str));
    assertThat(snapshot.getParts()).hasSize(1);
    assertThat(getClassOriginTrackerContent(snapshot.getParts().get(0)))
        .isEqualTo("com.coekie.flowtracker.test.ClassTest");
  }
}
