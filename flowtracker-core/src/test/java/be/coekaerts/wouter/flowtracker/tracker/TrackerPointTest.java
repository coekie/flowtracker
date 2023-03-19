package be.coekaerts.wouter.flowtracker.tracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TrackerPointTest {
  private Tracker source;

  @Before
  public void setupSource() {
    source = new FixedOriginTracker(1000);
  }

  @Test
  public void test() {
    TrackerPoint point = TrackerPoint.of(source, 5);
    assertSame(source, point.tracker);
    assertEquals(5, point.index);
  }

  @Test
  public void testMiddleman() {
    DefaultTracker middleman = new DefaultTracker();
    middleman.setSource(5, 10, source, 3, Growth.DOUBLE);
    TrackerPoint point = TrackerPoint.of(middleman, 5);
    assertSame(source, point.tracker);
    assertEquals(3, point.index);
    assertEquals(Growth.DOUBLE, point.growth);
  }

  @Test
  @Ignore // TODO[growth]. this doesn't work because of DefaultTracker growth handling
  public void testMiddlemanGrowth() {
    DefaultTracker middleman = new DefaultTracker();
    middleman.setSource(5, 10, source, 3, Growth.DOUBLE);
    TrackerPoint point = TrackerPoint.of(middleman, 7);
    assertSame(source, point.tracker);
    assertEquals(4, point.index);
    assertEquals(Growth.DOUBLE, point.growth);
  }
}
