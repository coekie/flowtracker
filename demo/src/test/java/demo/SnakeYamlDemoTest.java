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

    out.assertThatPart("myField").comesFromConstantInClass(SnakeYamlDemo.Pojo.class);
    out.assertThatPart("hello").comesFromConstantInClass(SnakeYamlDemo.class);

    out.assertThatPart("toDump").comesFromConstantInClass(SnakeYamlDemo.class);
    out.assertThatPart(":").comesFromConstantInClassThat()
        .startsWith("class org.yaml.snakeyaml");
  }
}