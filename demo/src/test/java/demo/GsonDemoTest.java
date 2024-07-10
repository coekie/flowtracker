package demo;

import com.google.gson.stream.JsonWriter;
import demo.DemoTestRule.TrackerSubject;
import org.junit.Rule;
import org.junit.Test;

public class GsonDemoTest {
  @Rule
  public DemoTestRule demo = new DemoTestRule();

  @Test
  public void test() {
    GsonDemo.main();
    TrackerSubject out = demo.out();
    out.assertThatPart("Pojo(").comesFromConstantInClass(GsonDemo.Pojo.class);
    out.assertThatPart("fromJson").comesFromConstantInClass(GsonDemo.class);
    out.assertThatPart("myField").comesFromConstantInClass(GsonDemo.Pojo.class);

    out.assertThatPart(":").comesFromConstantInClass(JsonWriter.class);
    out.assertThatPart("{").comesFromConstantInClass(JsonWriter.class);

    out.assertThatPart("toJson").comesFromConstantInClass(GsonDemo.class);
  }
}