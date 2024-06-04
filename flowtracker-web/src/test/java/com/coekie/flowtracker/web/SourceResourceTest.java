package com.coekie.flowtracker.web;

import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.web.SourceResource.SourceResponse;
import java.io.IOException;
import org.junit.Test;

public class SourceResourceTest {
  @Test
  public void test() throws IOException {
    ClassOriginTracker tracker = ClassOriginTracker.registerClass(
        SourceResourceTest.class.getClassLoader(),
        SourceResourceTest.class.getName().replace('.', '/'), null);
    InterestRepository.register(tracker);

    SourceResponse response = new SourceResource().get(tracker.getTrackerId());
    assertThat(response.lines).hasSize(1);
    assertThat(response.lines.get(0).line).isNull();
    assertThat(response.lines.get(0).content)
        .contains("INVOKEVIRTUAL java/lang/Class.getName ()Ljava/lang/String;");
  }
}