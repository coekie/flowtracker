package demo;

import org.junit.Rule;
import org.junit.Test;

public class HelloWorldTest {
  @Rule public DemoTestRule demo = new DemoTestRule();

  @Test
  public void test() {
    HelloWorld.main();
    demo.assertOutputComesFromConstantIn("Hello, world!", HelloWorld.class);
  }
}
