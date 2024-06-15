package com.coekie.flowtracker.web;

import static com.coekie.flowtracker.web.CodeResourceTest.findLine;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.web.CodeResource.CodeResponse;
import com.coekie.flowtracker.web.CodeResource.Line;
import java.util.List;
import org.junit.Test;

public class VineflowerCodeGeneratorTest {
  @Test
  public void test() {
    ClassOriginTracker tracker = ClassOriginTracker.registerClass(
        ExampleForSource.class.getClassLoader(),
        ExampleForSource.class.getName().replace('.', '/'), null);
    tracker.registerConstantString("line 9", 9);
    tracker.registerConstantString("line 16", 16);
    tracker.registerConstantString("line 17", 17);

    CodeResponse response = VineflowerCodeGenerator.getCode(List.of(tracker))
        .get(tracker.getTrackerId());

    assertThat(response).isNotNull();
    assertThat(response.lines).hasSize(17);

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
    assertThat(findLine(response, 13).content)
        .isEqualTo("      if (System.currentTimeMillis() > 0L) {\n");

    Line line16 = findLine(response, 16);
    assertThat(line16.content)
        .isEqualTo("      System.out.println(\"line 16\" + System.currentTimeMillis() + \"line 17\");\n");
    assertThat(line16.parts).hasSize(2); // "line 16" and "line 18"
  }
}