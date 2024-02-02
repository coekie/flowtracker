package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.TagTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
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
    return TrackerRepository.getTracker(is);
  }

  @Test
  public void descriptor() throws IOException {
    ByteArrayInputStream src = new ByteArrayInputStream(deflated);
    TrackerRepository.setTracker(src, new TagTracker("src").addTo(TrackerTree.node("src")));

    try (InputStream is = new InflaterInputStream(src)) {
      TrackTestHelper.assertThatTracker(is)
          .hasDescriptor("InflaterInputStream for src")
          .hasNode("src", "Inflater");
    }

    try (InputStream is = new InflaterInputStream(src, new Inflater())) {
      TrackTestHelper.assertThatTracker(is)
          .hasDescriptor("InflaterInputStream for src")
          .hasNode("src", "Inflater");
    }

    try (InputStream is = new InflaterInputStream(src, new Inflater(), 512)) {
      TrackTestHelper.assertThatTracker(is)
          .hasDescriptor("InflaterInputStream for src")
          .hasNode("src", "Inflater");
    }
  }
}
