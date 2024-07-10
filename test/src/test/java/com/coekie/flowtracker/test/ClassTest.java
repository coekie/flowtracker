package com.coekie.flowtracker.test;

import com.coekie.flowtracker.hook.InputStreamHook;
import java.io.InputStream;
import org.junit.Test;

/**
 * @see ReflectionTest
 */
public class ClassTest {
  @Test
  public void getResourceAsStream() {
    InputStream stream = URLTest.class.getResourceAsStream("ClassTest.class");
    TrackTestHelper.assertThatTrackerNode(InputStreamHook.getInputStreamTracker(stream))
        .hasPathStartingWith("Files")
        .hasPathEndingWith("ClassTest.class");
  }
}
