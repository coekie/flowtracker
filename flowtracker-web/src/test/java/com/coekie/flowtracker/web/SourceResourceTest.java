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
    InterestRepository.register(tracker);

    SourceResponse response = new SourceResource().get(tracker.getTrackerId());

    assertThat(response.lines).hasSize(9);
    assertThat(findLine(response, 9).content).isEqualTo("   L0\n"
        + "    LINENUMBER 9 L0\n"
        + "    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;\n"
        + "    LDC \"line 9\"\n"
        + "    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V\n");
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