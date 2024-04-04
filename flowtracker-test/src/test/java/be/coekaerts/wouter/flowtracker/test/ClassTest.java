package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.assertThatTracker;

import be.coekaerts.wouter.flowtracker.hook.InputStreamHook;
import java.io.InputStream;
import org.junit.Test;

public class ClassTest {
  @Test public void getResourceAsStream() {
    InputStream stream = URLTest.class.getResourceAsStream("ClassTest.class");
    assertThatTracker(InputStreamHook.getInputStreamTracker(stream))
        .hasNodeStartingWith("Files")
        .hasNodeEndingWith("ClassTest.class");
  }
}
