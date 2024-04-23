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
    demo.assertOutputComesFromConstantIn("toJson", GsonDemo.class);
    demo.assertOutputComesFromConstantIn("fromJson", GsonDemo.class);
    demo.assertOutputComesFromConstantIn(":", JsonWriter.class);
    demo.assertOutputNotTracked("Pojo("); // got lost in StringConcatFactory
    demo.assertOutputNotTracked("{"); // haven't looked into why this isn't tracked yet
  }
}