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

    // class and field names (Class.getName() and Field.getName()) are tracked
    out.assertThatPart("demo.SerializationDemo$Pojo")
        .comesFromConstantInClass(SerializationDemo.Pojo.class);
    out.assertThatPart("myField")
        .comesFromConstantInClass(SerializationDemo.Pojo.class);

    // not tracked because of the call to String.intern() in java.io.ObjectStreamField.getSignature
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