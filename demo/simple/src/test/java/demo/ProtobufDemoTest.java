package demo;

import com.google.protobuf.Method;
import demo.DemoTestRule.TrackerSubject;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;

public class ProtobufDemoTest {
  @Rule
  public DemoTestRule demo = new DemoTestRule();

  @Test
  public void test() throws IOException {
    ProtobufDemo.main();
    TrackerSubject out = demo.out();
    // field number (annoyingly happens to be a newline character
    out.assertThatPart("\n").comesFromConstantInClassThat()
        .startsWith("class com.google.protobuf.CodedOutputStream$ArrayEncoder");
    // string length
    out.assertThatPart(new byte[] {6}).comesFromConstantInClassThat()
        .startsWith("class com.google.protobuf.CodedOutputStream$ArrayEncoder");
    // don't know yet why this isn't tracked
    out.assertThatPart("myName").isNotTracked();
    // 1 = Syntax.SYNTAX_PROTO3.getNumber()
    out.assertThatPart(new byte[] {1}).comesFromConstantInClass(Method.class);
  }
}