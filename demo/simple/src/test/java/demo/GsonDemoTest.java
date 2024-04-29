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
    demo.assertOutputComesFromConstantIn("Pojo(", GsonDemo.Pojo.class);
    demo.assertOutputComesFromConstantIn("fromJson", GsonDemo.class);

    demo.assertOutputNotTracked("myField"); // names of fields (through reflection) are not tracked
    demo.assertOutputComesFromConstantIn(":", JsonWriter.class);
    demo.assertOutputNotTracked("{"); // haven't looked into why this isn't tracked yet
    demo.assertOutputComesFromConstantIn("toJson", GsonDemo.class);
  }
}