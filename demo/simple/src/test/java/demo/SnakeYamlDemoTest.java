package demo;

import org.junit.Rule;
import org.junit.Test;

public class SnakeYamlDemoTest {
  @Rule
  public DemoTestRule demo = new DemoTestRule();

  @Test
  public void test() {
    SnakeYamlDemo.main();
    // TODO handle org.yaml.snakeyaml.reader.StreamReader codepoint usage
    //  demo.assertOutputComesFromConstantIn("hello", SnakeYamlDemo.class);
    demo.assertOutputNotTracked("hello");
    demo.assertOutputNotTracked("myField"); // names of fields (through reflection) are not tracked

    demo.assertOutputComesFromConstantIn("toDump", SnakeYamlDemo.class);
    demo.assertOutputComesFromConstantInClassThat(":").startsWith("class org.yaml.snakeyaml");
  }
}