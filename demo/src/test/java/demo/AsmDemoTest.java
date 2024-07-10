package demo;

import demo.DemoTestRule.TrackerSubject;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassWriter;

public class AsmDemoTest {
  @Rule
  public DemoTestRule demo = new DemoTestRule();

  @Test
  public void test() throws IOException {
    AsmDemo.main();
    TrackerSubject out = demo.out();
    // magic
    out.assertThatPart(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE})
        .comesFromConstantInClass(ClassWriter.class);
    // length-prefix of a UTF-8 String
    out.assertThatPart(new byte[] {(byte) 0x0F})
        .comesFromConstantInClass(ByteVector.class);
    out.assertThatPart("LineNumberTable").comesFromConstantInClassThat()
        .startsWith("class org.objectweb.asm.MethodWriter");

    // Strings read from original class file
    out.assertThatPart("java/lang/System").comesFromTrackerWithPath()
        .contains("HelloWorld.class");
  }
}