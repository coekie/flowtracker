package demo;

import com.google.gson.stream.JsonWriter;
import org.junit.Rule;
import org.junit.Test;

public class GsonDemoTest {
  @Rule
  public DemoTestRule demo = new DemoTestRule();

  @Test
  public void test() {
    GsonDemo.main();
    demo.assertThatOutput("Pojo(").comesFromConstantInClass(GsonDemo.Pojo.class);
    demo.assertThatOutput("fromJson").comesFromConstantInClass(GsonDemo.class);

    // names of fields (through reflection) are not tracked
    demo.assertThatOutput("myField").isNotTracked();
    demo.assertThatOutput(":").comesFromConstantInClass(JsonWriter.class);
    // haven't looked into why this isn't tracked yet
    demo.assertThatOutput("{").isNotTracked();

    demo.assertThatOutput("toJson").comesFromConstantInClass(GsonDemo.class);
  }
}