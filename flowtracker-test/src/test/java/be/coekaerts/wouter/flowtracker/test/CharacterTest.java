package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackedCharArray;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerRepository.getTracker;
import static com.google.common.truth.Truth.assertThat;

import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import org.junit.Test;

/** Tests for tracking methods in the {@link Character} class */
public class CharacterTest {
  @Test
  public void testToCodePointCombination() {
    FlowTester ft = new FlowTester().withIndex(7).withLength(3);
    FlowTester ft2 = ft.withIndex(10).withLength(2);

    int codePoint = Character.toCodePoint(ft.createSourceChar('\ud83c'),
        ft2.createSourceChar('\udf09'));
    assertThat(FlowTester.getIntSourcePoint(codePoint))
        .isEqualTo(TrackerPoint.of(ft.tracker(), 7, 3 + 2));
  }

  @Test
  public void testToCodePointCombinationWithDifferentTrackers() {
    FlowTester ft = new FlowTester().withIndex(7).withLength(3);
    FlowTester ft2 = new FlowTester().withIndex(10).withLength(2);

    int codePoint = Character.toCodePoint(ft.createSourceChar('\ud83c'),
        ft2.createSourceChar('\udf09'));
    assertThat(FlowTester.getIntSourcePoint(codePoint))
        .isEqualTo(ft.point());
  }

  @Test
  public void testToCodePointCombinationWithMismatchingIndexes() {
    FlowTester ft = new FlowTester().withIndex(7).withLength(3);
    FlowTester ft2 = ft.withIndex(11).withLength(2);

    int codePoint = Character.toCodePoint(ft.createSourceChar('\ud83c'),
        ft2.createSourceChar('\udf09'));
    assertThat(FlowTester.getIntSourcePoint(codePoint))
        .isEqualTo(ft.point());
  }

  @Test
  public void testCodePointAt() {
    char[] abc = trackedCharArray("abc");
    int b = Character.codePointAt(abc, 1);
    FlowTester.assertTrackedValue(b, 'b', getTracker(abc), 1);
  }

  @Test
  public void testCodePointAtWithSurrogatePair() {
    char[] abc = trackedCharArray("a\ud83c\udf09c");
    int b = Character.codePointAt(abc, 1);
    FlowTester.assertTrackedValue(b, 127753, getTracker(abc), 1);
    assertThat(FlowTester.getIntSourcePoint(b))
        .isEqualTo(TrackerPoint.of(getTracker(abc), 1, 2));
  }
}
