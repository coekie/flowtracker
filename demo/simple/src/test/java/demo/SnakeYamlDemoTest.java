package demo;

import org.junit.Rule;
import org.junit.Test;

public class SnakeYamlDemoTest {
  @Rule
  public DemoTestRule demo = new DemoTestRule();

  @Test
  public void test() {
    SnakeYamlDemo.main();
    demo.assertThatOutput("hello").comesFromConstantInClass(SnakeYamlDemo.class);
    // names of fields (through reflection) are not tracked
    demo.assertThatOutput("myField").isNotTracked();

    demo.assertThatOutput("toDump").comesFromConstantInClass(SnakeYamlDemo.class);
    demo.assertThatOutput(":").comesFromConstantInClassThat()
        .startsWith("class org.yaml.snakeyaml");
  }
}