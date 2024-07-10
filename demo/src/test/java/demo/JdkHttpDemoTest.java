package demo;

import com.coekie.flowtracker.tracker.TrackerTree;
import demo.DemoTestRule.TrackerSubject;
import org.junit.Rule;
import org.junit.Test;

public class JdkHttpDemoTest {
  @Rule
  public DemoTestRule demo = new DemoTestRule();

  @Test
  public void test() throws Exception {
    JdkHttpDemo.main();

    TrackerSubject clientSent = demo.newSink(TrackerTree.node("Client socket"));
    clientSent.assertThatPart("/test").comesFromConstantInClass(JdkHttpDemo.class);
    clientSent.assertThatPart("GET").comesFromConstantInClassThat()
        .startsWith("class jdk.internal.net.http");
    clientSent.assertThatPart("HTTP").comesFromConstantInClassThat()
        .startsWith("class jdk.internal.net.http");
    clientSent.assertThatPart("Connection").comesFromConstantInClassThat()
        .startsWith("class jdk.internal.net.http");
    clientSent.assertThatPart("User-Agent").comesFromConstantInClassThat()
        .startsWith("class jdk.internal.net.http");

    TrackerSubject serverSent = demo.newSink(TrackerTree.node("Server socket"));
    serverSent.assertThatPart("This is the response")
        .comesFromConstantInClass(JdkHttpDemo.MyHandler.class);
    serverSent.assertThatPart("HTTP").comesFromConstantInClassThat()
        .startsWith("class sun.net.httpserver");
    serverSent.assertThatPart("OK").comesFromConstantInClassThat()
        .startsWith("class sun.net.httpserver");
    serverSent.assertThatPart("Content-length").comesFromConstantInClassThat()
        .startsWith("class sun.net.httpserver");
  }
}