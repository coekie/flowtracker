package com.coekie.flowtracker.web;

import static com.coekie.flowtracker.web.SourceResourceTest.findLine;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.web.SourceResource.Line;
import com.coekie.flowtracker.web.SourceResource.SourceResponse;
import java.io.IOException;
import org.junit.Test;

public class VineflowerSourceGeneratorTest {
  @Test
  public void test() throws IOException {
    ClassOriginTracker tracker = ClassOriginTracker.registerClass(
        ExampleForSource.class.getClassLoader(),
        ExampleForSource.class.getName().replace('.', '/'), null);
    tracker.registerConstantString("line 9", 9);

    SourceResponse response = VineflowerSourceGenerator.getSource(tracker);

    assertThat(response).isNotNull();
    assertThat(response.lines).hasSize(15);

    // simple line
    Line line9 = findLine(response, 9);
    assertThat(line9.content).isEqualTo("      System.out.println(\"line 9\");\n");
    assertThat(line9.parts).hasSize(1);
    assertThat(line9.parts.get(0).tracker.id).isEqualTo(tracker.getTrackerId());
    assertThat(tracker.getContent().subSequence(
        line9.parts.get(0).offset,
        line9.parts.get(0).offset + line9.parts.get(0).length).toString())
        .isEqualTo("line 9");

    // line with multiple labels
    // TODO this should contain more; also the lines after it. but we don't have a good way of
    //  determining where in the decompiler output that the code for that line in the original
    //  source ends.
    assertThat(findLine(response, 12).content)
        .isEqualTo("      if (System.currentTimeMillis() > 0L) {\n");
  }
}