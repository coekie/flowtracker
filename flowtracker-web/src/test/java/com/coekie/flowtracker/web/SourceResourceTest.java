package com.coekie.flowtracker.web;

import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.web.SourceResource.Line;
import com.coekie.flowtracker.web.SourceResource.SourceResponse;
import java.io.IOException;
import java.util.Objects;
import org.junit.Test;

public class SourceResourceTest {
  @Test
  public void test() throws IOException {
    ClassOriginTracker tracker = ClassOriginTracker.registerClass(
        ExampleForSource.class.getClassLoader(),
        ExampleForSource.class.getName().replace('.', '/'), null);
    tracker.registerConstantString("line 9", 9);
    InterestRepository.register(tracker);

    SourceResponse response = new SourceResource().get(tracker.getTrackerId());

    assertThat(response.lines).hasSize(9);

    // simple line
    Line line9 = findLine(response, 9);
    assertThat(line9.content).isEqualTo("   L0\n"
        + "    LINENUMBER 9 L0\n"
        + "    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;\n"
        + "    LDC \"line 9\"\n"
        + "    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V\n");
    assertThat(line9.parts).hasSize(1);
    assertThat(line9.parts.get(0).tracker.id).isEqualTo(tracker.getTrackerId());
    assertThat(tracker.getContent().subSequence(
        line9.parts.get(0).offset,
            line9.parts.get(0).offset + line9.parts.get(0).length).toString())
        .isEqualTo("line 9");

    // line with multiple labels
    assertThat(findLine(response, 12).content).isEqualTo("   L2\n"
        + "    LINENUMBER 12 L2\n"
        + "    INVOKESTATIC java/lang/System.currentTimeMillis ()J\n"
        + "    LCONST_0\n"
        + "    LCMP\n"
        + "    IFLE L3\n"
        + "    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;\n"
        + "    LDC \"t\"\n"
        + "    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V\n"
        + "    GOTO L4\n"
        + "   L3\n"
        + "    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;\n"
        + "    LDC \"f\"\n"
        + "    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V\n");
  }

  private Line findLine(SourceResponse response, int lineNumber) {
    return response.lines.stream()
        .filter(line -> Objects.equals(line.line, lineNumber))
        .findFirst()
        .orElseThrow();
  }
}