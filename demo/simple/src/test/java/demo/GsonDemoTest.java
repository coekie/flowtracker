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

    // names of fields (through reflection) are not tracked
    out.assertThatPart("myField").isNotTracked();
    out.assertThatPart(":").comesFromConstantInClass(JsonWriter.class);
    // haven't looked into why this isn't tracked yet
    out.assertThatPart("{").isNotTracked();

    out.assertThatPart("toJson").comesFromConstantInClass(GsonDemo.class);
  }
}