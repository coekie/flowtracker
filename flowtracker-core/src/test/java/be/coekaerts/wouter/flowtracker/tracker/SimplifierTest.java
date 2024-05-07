package be.coekaerts.wouter.flowtracker.tracker;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshot;
import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class SimplifierTest {
  private final Tracker source = new FixedOriginTracker(1000);
  private final Tracker source2 = new FixedOriginTracker(1000);

  @Test
  public void testSimpleMerge() {
    assertThat(snapshot().part(2, source, 1).part(2, source, 3).build().simplify())
        .isEqualTo(snapshot().part(4, source, 1).build());
  }

  @Test
  public void testSimpleMergeGrowth() {
    assertThat(snapshot()
        .part(10, source, 1, Growth.of(10, 1))
        .part(10, source, 2, Growth.of(10, 1)).build().simplify())
        .isEqualTo(snapshot().part(20, source, 1, Growth.of(10, 1)).build());
  }

  @Test
  public void testExpandWithGrowth() {
    assertThat(snapshot()
        .part(2, source, 1, Growth.of(2, 1))
        .part(3, source, 2, Growth.of(3, 1)).build().simplify())
        .isEqualTo(snapshot().part(5, source, 1, Growth.of(5, 2)).build());
  }

  @Test
  public void testRepeated() {
    assertThat(snapshot()
        .part(1, source, 1)
        .part(1, source, 1)
        .part(1, source, 1)
        .build().simplify())
        .isEqualTo(snapshot().part(3, source, 1, Growth.of(3, 1)).build());
  }

  @Test
  public void testDoNotMergeWrongSourceIndex() {
    assertNoChange(snapshot().part(2, source, 1).part(2, source, 5));
  }

  @Test
  public void testDoNotMergeDifferentTracker() {
    assertNoChange(snapshot().part(2, source, 1).part(2, source2, 3));
  }

  private void assertNoChange(TrackerSnapshot.Builder snapshotBuilder) {
    TrackerSnapshot snapshot = snapshotBuilder.build();
    assertThat(snapshot.simplify()).isEqualTo(snapshot);
  }
}