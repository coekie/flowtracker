package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class FilterInputStreamTest {
  // BufferedInputStream is a concrete subclass of FilterInputStream
  @Test public void bufferedInputStream() {
    ByteArrayInputStream source = new ByteArrayInputStream(new byte[1]);
    TrackerRepository.createTracker(source).initDescriptor("mySource", null);
    BufferedInputStream buffered = new BufferedInputStream(source);
    Tracker tracker = TrackerRepository.getTracker(buffered);
    assertEquals("BufferedInputStream", tracker.getDescriptor());
    assertSame(TrackerRepository.getTracker(source), tracker.getDescriptorTracker());
  }
}
