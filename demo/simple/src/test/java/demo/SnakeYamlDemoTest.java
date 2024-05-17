package demo;

import demo.DemoTestRule.TrackerSubject;
import org.junit.Rule;
import org.junit.Test;

public class SnakeYamlDemoTest {
  @Rule
  public DemoTestRule demo = new DemoTestRule();

  @Test
  public void test() {
    SnakeYamlDemo.main();
    TrackerSubject out = demo.out();

    out.assertThatPart("hello").comesFromConstantInClass(SnakeYamlDemo.class);
    // names of fields (through reflection) are not tracked
    out.assertThatPart("myField").isNotTracked();

    out.assertThatPart("toDump").comesFromConstantInClass(SnakeYamlDemo.class);
    out.assertThatPart(":").comesFromConstantInClassThat()
        .startsWith("class org.yaml.snakeyaml");
  }
}