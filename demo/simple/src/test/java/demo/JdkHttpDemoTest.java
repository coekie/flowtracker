package demo;

import com.coekie.flowtracker.tracker.TrackerTree;
import demo.DemoTestRule.TrackerSubject;
import org.junit.Rule;
import org.junit.Test;

public class JdkHttpDemoTest {
  @Rule
  public DemoTestRule demo = new DemoTestRule();

  @Test
  public void test() throws Exception {
    JdkHttpDemo.main();

    TrackerSubject clientSent = demo.newSink(TrackerTree.node("Client socket"));

    // TODO fix tracking on HttpClient
    // clientSent.assertThatPart("HTTP").isNotTracked();
  }
}