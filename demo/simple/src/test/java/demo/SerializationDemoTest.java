package demo;

import demo.DemoTestRule.TrackerSubject;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import org.junit.Rule;
import org.junit.Test;

public class SerializationDemoTest {
  @Rule
  public DemoTestRule demo = new DemoTestRule();

  @Test
  public void test() throws Exception {
    SerializationDemo.main();
    TrackerSubject out = demo.out();

    out.assertThatPart("myValue").comesFromConstantInClass(SerializationDemo.class);
    out.assertThatPart("Deserialized: ", "myValue")
        .comesFromConstantInClass(SerializationDemo.class);

    // names of fields and classes (through reflection) are not tracked
    out.assertThatPart("myField").isNotTracked();
    out.assertThatPart("java/lang/String").isNotTracked();

    // magic, from ObjectOutputStream.writeStreamHeader
    out.assertThatPart(new byte[]{(byte) 0xac, (byte) 0xed})
        .comesFromConstantInClass(ObjectOutputStream.class);
    // version, from ObjectOutputStream.writeStreamHeader
    out.assertThatPart(new byte[]{(byte) 5})
        .comesFromConstantInClass(ObjectOutputStream.class);

    // from ObjectOutputStream.writeOrdinaryObject ('s')
    out.assertThatPart("s").comesFromConstantInClass(ObjectOutputStream.class);
    out.assertThatPart("Serialized: ".getBytes(), new byte[]{ObjectStreamConstants.TC_OBJECT})
        .comesFromConstantInClass(ObjectOutputStream.class);

    // from ObjectOutputStream.writeNonProxyDesc ('r')
    out.assertThatPart("Serialized: ".getBytes(), new byte[]{ObjectStreamConstants.TC_CLASSDESC})
        .comesFromConstantInClass(ObjectOutputStream.class);
  }
}