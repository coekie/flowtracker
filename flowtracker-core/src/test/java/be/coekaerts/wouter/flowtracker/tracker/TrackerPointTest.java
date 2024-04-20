package be.coekaerts.wouter.flowtracker.tracker;

import static com.google.common.truth.Truth.assertThat;

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
    assertThat(point.tracker).isSameInstanceAs(source);
    assertThat(point.index).isEqualTo(5);
  }

  @Test
  public void testMiddleman() {
    DefaultTracker middleman = new DefaultTracker();
    middleman.setSource(5, 10, source, 3, Growth.DOUBLE);
    TrackerPoint point = TrackerPoint.of(middleman, 5);
    assertThat(point.tracker).isSameInstanceAs(source);
    assertThat(point.index).isEqualTo(3);
    assertThat(point.growth).isEqualTo(Growth.DOUBLE);
  }

  @Test
  @Ignore // TODO[growth]. this doesn't work because of DefaultTracker growth handling
  public void testMiddlemanGrowth() {
    DefaultTracker middleman = new DefaultTracker();
    middleman.setSource(5, 10, source, 3, Growth.DOUBLE);
    TrackerPoint point = TrackerPoint.of(middleman, 7);
    assertThat(point.tracker).isSameInstanceAs(source);
    assertThat(point.index).isEqualTo(4);
    assertThat(point.growth).isEqualTo(Growth.DOUBLE);
  }
}
