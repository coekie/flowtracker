package demo;

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

    demo.assertThatOutput("myValue").comesFromConstantInClass(SerializationDemo.class);
    demo.assertThatOutput("Deserialized: ", "myValue")
        .comesFromConstantInClass(SerializationDemo.class);

    // names of fields and classes (through reflection) are not tracked
    demo.assertThatOutput("myField").isNotTracked();
    demo.assertThatOutput("java/lang/String").isNotTracked();

    // magic, from ObjectOutputStream.writeStreamHeader
    demo.assertThatOutput(new byte[]{(byte) 0xac, (byte) 0xed})
            .isNotTracked();
        // TODO .comesFromConstantInClass(ObjectOutputStream.class);
    // version, from ObjectOutputStream.writeStreamHeader
    demo.assertThatOutput(new byte[]{(byte) 5})
        .comesFromConstantInClass(ObjectOutputStream.class);

    // from ObjectOutputStream.writeOrdinaryObject ('s')
    demo.assertThatOutput("s").comesFromConstantInClass(ObjectOutputStream.class);
    demo.assertThatOutput("Serialized: ".getBytes(), new byte[]{ObjectStreamConstants.TC_OBJECT})
        .comesFromConstantInClass(ObjectOutputStream.class);

    // from ObjectOutputStream.writeNonProxyDesc ('r')
    demo.assertThatOutput("Serialized: ".getBytes(), new byte[]{ObjectStreamConstants.TC_CLASSDESC})
        .comesFromConstantInClass(ObjectOutputStream.class);
  }
}