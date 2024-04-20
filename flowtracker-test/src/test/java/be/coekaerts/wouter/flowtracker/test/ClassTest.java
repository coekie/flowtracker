package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.assertThatTrackerNode;

import be.coekaerts.wouter.flowtracker.hook.InputStreamHook;
import java.io.InputStream;
import org.junit.Test;

public class ClassTest {
  @Test public void getResourceAsStream() {
    InputStream stream = URLTest.class.getResourceAsStream("ClassTest.class");
    assertThatTrackerNode(InputStreamHook.getInputStreamTracker(stream))
        .hasPathStartingWith("Files")
        .hasPathEndingWith("ClassTest.class");
  }
}
