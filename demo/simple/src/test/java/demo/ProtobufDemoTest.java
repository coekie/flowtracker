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
    // tag / field number (annoyingly happens to be a newline character)
    out.assertThatPart("\n").comesFromConstantInClassThat()
        .startsWith("class com.google.protobuf.CodedOutputStream$ArrayEncoder");
    // string length
    out.assertThatPart(new byte[] {4}).comesFromConstantInClassThat()
        .startsWith("class com.google.protobuf.CodedOutputStream$ArrayEncoder");
    // strings
    out.assertThatPart("name").comesFromConstantInClass(ProtobufDemo.class);
    out.assertThatPart("option").comesFromConstantInClass(ProtobufDemo.class);
    // enum value. 1 == Syntax.SYNTAX_PROTO3.getNumber()
    out.assertThatPart(new byte[] {1}).comesFromConstantInClass(Method.class);
  }
}