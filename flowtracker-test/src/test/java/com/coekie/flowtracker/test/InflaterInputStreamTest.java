package com.coekie.flowtracker.test;

import static com.coekie.flowtracker.tracker.Context.context;

import com.coekie.flowtracker.tracker.TagTracker;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerRepository;
import com.coekie.flowtracker.tracker.TrackerTree;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.junit.Test;

public class InflaterInputStreamTest extends AbstractInputStreamTest {
  private static final byte[] deflated = createDeflated();

  static byte[] createDeflated() {
    var bout = new ByteArrayOutputStream();
    try (var dos = new DeflaterOutputStream(bout)) {
      dos.write("abc".getBytes());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return bout.toByteArray();
  }

  @Override
  InputStream createInputStream(byte[] bytes) {
    return new InflaterInputStream(new ByteArrayInputStream(deflated));
  }

  @Override
  Tracker getStreamTracker(InputStream is) {
    return TrackerRepository.getTracker(context(), is);
  }

  @Test
  public void node() throws IOException {
    ByteArrayInputStream src = new ByteArrayInputStream(deflated);
    TrackerRepository.setTracker(context(), src, new TagTracker().addTo(TrackerTree.node("src")));

    try (InputStream is = new InflaterInputStream(src)) {
      TrackTestHelper.assertThatTrackerNode(is)
          .hasPath("src", "Inflater");
    }

    try (InputStream is = new InflaterInputStream(src, new Inflater())) {
      TrackTestHelper.assertThatTrackerNode(is)
          .hasPath("src", "Inflater");
    }

    try (InputStream is = new InflaterInputStream(src, new Inflater(), 512)) {
      TrackTestHelper.assertThatTrackerNode(is)
          .hasPath("src", "Inflater");
    }
  }
}
