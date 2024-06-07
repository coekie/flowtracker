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
    assertThat(findLine(response, 9).content).contains("line 9");
  }

  static Line findLine(SourceResponse response, int lineNumber) {
    return response.lines.stream()
        .filter(line -> Objects.equals(line.line, lineNumber))
        .findFirst()
        .orElseThrow();
  }
}