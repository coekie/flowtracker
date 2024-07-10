package com.coekie.flowtracker.tracker;

import static com.coekie.flowtracker.tracker.TrackerSnapshot.assertThatTracker;
import static com.coekie.flowtracker.tracker.TrackerSnapshot.snapshot;

import org.junit.Before;
import org.junit.Test;

public class TrackerTest {
  private Tracker source, source2, target, middleman;

  @Before public void setupSource() {
    source = new FakeOriginTracker(1000);
    source2 = new FakeOriginTracker(1000);
    target = new DefaultTracker();
    middleman = new DefaultTracker();
  }

  @Test public void testSetSingleSource() {
    target.setSource(5, 3, source, 105); // setting 5,6,7

    assertThatTracker(target).matches(snapshot().gap(5).part(3, source, 105));
  }

  @Test public void testSetSingleSourceOtherGrowth() {
    target.setSource(5, 4, source, 105, Growth.DOUBLE); // setting 5,6,7,8

    assertThatTracker(target).matches(snapshot().gap(5).part(4, source, 105, Growth.DOUBLE));
  }

  /** Set one source, then a second one after it, leaving a gap in between */
  @Test public void setSecondSourceAfterGap() {
    target.setSource(5, 3, source, 105); // setting 5,6,7
    target.setSource(9, 2, source, 109); // setting 9,10, leaving a gap at 8

    assertThatTracker(target).matches(
        snapshot().gap(5).part(3, source, 105).gap(1).part(2, source, 109));
  }

  /** Set one source, then a second one before it, leaving a gap in between */
  @Test public void setSecondSourceBeforeGap() {
    target.setSource(5, 3, source, 105); // setting 5,6,7
    target.setSource(2, 2, source, 102); // setting 2,3, leaving a gap at 4

    assertThatTracker(target).matches(
        snapshot().gap(2).part(2, source, 102).gap(1).part(3, source, 105));
  }

  /** Merge an entry if it comes after an existing one that matches */
  @Test public void testTouchMergeWithPrevious() {
    target.setSource(5, 3, source, 105); // setting 5,6,7
    target.setSource(8, 2, source, 108); // setting 8,9

    assertThatTracker(target).matches(snapshot().gap(5).part(5, source, 105));
  }

  /** Don't merge an entry if it comes right after one, but the indices don't match */
  @Test public void testTouchDontMergeWithPreviousWhenSkipped() {
    target.setSource(5, 3, source, 105); // setting 5,6,7 to 105,...
    target.setSource(8, 2, source, 109); // setting 8,9 to 109,... (skipping 108)

    assertThatTracker(target).matches(snapshot().gap(5).part(3, source, 105).part(2, source, 109));
  }

  /** Merge an entry if it comes before an existing one that matches */
  @Test public void testTouchMergeWithNext() {
    target.setSource(5, 3, source, 105); // setting 5,6,7
    target.setSource(3, 2, source, 103); // setting 3,4

    assertThatTracker(target).matches(snapshot().gap(3).part(5, source, 103));
  }

  /** Don't merge an entry if it comes right before one, but the indices don't match */
  @Test public void testTouchDontMergeWithNextWhenSkipped() {
    target.setSource(5, 3, source, 105); // setting 5,6,7
    target.setSource(3, 2, source, 102); // setting 3,4

    assertThatTracker(target).matches(snapshot().gap(3).part(2, source, 102).part(3, source, 105));
  }

  /** Don't merge entries with a different source */
  @Test public void testDontMergeDifferentSource() {
    target.setSource(5, 3, source, 105); // setting 5,6,7
    target.setSource(8, 2, source2, 108); // setting 8,9

    assertThatTracker(target).matches(snapshot().gap(5).part(3, source, 105).part(2, source2, 108));
  }

  /** Insert the missing part in between two entries with a hole, expect the three to merge */
  @Test public void testTouchMergeWithPreviousAndNext() {
    target.setSource(10, 2, source, 110); // setting 10,11
    target.setSource(5, 3, source, 105); // setting 5,6,7
    target.setSource(8, 2, source, 108); // setting 8,9

    assertThatTracker(target).matches(snapshot().gap(5).part(7, source, 105));
  }

  @Test public void testOverlapMergeWithPrevious() {
    target.setSource(5, 3, source, 105); // setting 5,6,7
    target.setSource(6, 4, source, 106); // setting 6,7,8,9

    assertThatTracker(target).matches(snapshot().gap(5).part(5, source, 105));
  }

  @Test public void testOverlapMergeWithNext() {
    target.setSource(6, 4, source, 106); // setting 6,7,8,9
    target.setSource(5, 3, source, 105); // setting 5,6,7

    assertThatTracker(target).matches(snapshot().gap(5).part(5, source, 105));
  }

  @Test public void testOverlapOverwritePrevious() {
    target.setSource(5, 3, source, 105); // setting 5,6,7
    target.setSource(6, 4, source2, 106); // setting 6,7,8,9

    assertThatTracker(target).matches(snapshot().gap(5).part(1, source, 105).part(4, source2, 106));
  }

  @Test public void testOverlapOverwriteNext() {
    target.setSource(6, 4, source, 106); // setting 6,7,8,9
    target.setSource(5, 3, source2, 105); // setting 5,6,7

    assertThatTracker(target).matches(snapshot().gap(5).part(3, source2, 105).part(2, source, 108));
  }

  @Test public void testOverlapOverwriteCompletely() {
    target.setSource(5, 3, source, 105); // setting 5,6,7
    target.setSource(4, 5, source2, 104); // setting 4,5,6,7,8

    assertThatTracker(target).matches(snapshot().gap(4).part(5, source2, 104));
  }

  @Test public void testOverwriteSameRange() {
    target.setSource(5, 3, source, 105); // setting 5,6,7
    target.setSource(5, 3, source2, 105); // setting 5,6,7

    assertThatTracker(target).matches(snapshot().gap(5).part(3, source2, 105));
  }

  @Test public void testOverlapOverwriteMultiple() {
    target.setSource(5, 3, source, 10);
    target.setSource(8, 3, source, 20);
    target.setSource(11, 3, source, 30);
    target.setSource(14, 3, source, 40);
    target.setSource(6, 10, source, 100);

    assertThatTracker(target).matches(
        snapshot().gap(5).part(1, source, 10).part(10, source, 100).part(1, source, 42));
  }

  @Test public void testOverwriteMiddle() {
    target.setSource(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSource(6, 2, source2, 106); // setting 6,7

    assertThatTracker(target).matches(
        snapshot().gap(5).part(1, source, 105).part(2, source2, 106).part(2, source, 108));
  }

  @Test public void testOverwriteStart() {
    target.setSource(5, 5, source, 105);
    target.setSource(5, 1, source2, 105);

    assertThatTracker(target).matches(snapshot().gap(5).part(1, source2, 105).part(4, source, 106));
  }

  @Test public void testOverwriteEnd() {
    target.setSource(5, 5, source, 105);
    target.setSource(9, 1, source2, 109);

    assertThatTracker(target).matches(snapshot().gap(5).part(4, source, 105).part(1, source2, 109));
  }

  @Test public void testOverwriteWithSame() {
    target.setSource(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSource(6, 2, source, 106); // setting 6,7 to the same again

    assertThatTracker(target).matches(snapshot().gap(5).part(5, source, 105));
  }

  @Test public void testOverwriteWithUntracked() {
    target.setSource(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSource(6, 2, null, 999); // overwrite 6,7 with untracked object

    assertThatTracker(target).matches(
        snapshot().gap(5).part(1, source, 105).gap(2).part(2, source, 108));
  }

  @Test public void testOverwriteWithGap() {
    // middleman has known source at start and end, but not at where we're reading from it;
    // in other words we'll be reading from a gap in the middle of middleman
    middleman.setSource(0, 1, source2, 0);
    middleman.setSource(50, 1, source2, 0);

    target.setSource(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSource(6, 2, middleman, 20); // overwrite 6,7 with gap

    assertThatTracker(target).matches(
        snapshot().gap(5).part(1, source, 105).gap(2).part(2, source, 108));
  }

  @Test public void testOverwriteWithGapInTheMiddle() {
    // middleman has known source at start and end, but not entirely at where we're reading from it;
    // we'll be reading from a known part + gap + known part of middleman
    middleman.setSource(0, 21, source2, 200);
    middleman.setSource(22, 1000, source2, 300);

    target.setSource(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSource(6, 3, middleman, 20); // overwrite 6,7,8 where 7 is the gap

    assertThatTracker(target).matches(snapshot()
        .gap(5)
        .part(1, source, 105)
        .part(1, source2, 220)
        .gap(1)
        .part(1, source2, 300)
        .part(1, source, 109));
  }

  @Test public void testOverwriteWithGapInTheBeginning() {
    // middleman has known source at the end, but not entirely at where we're reading from it;
    // we'll be reading from a gap + known part of middleman
    middleman.setSource(21, 1000, source2, 200);

    target.setSource(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSource(6, 2, middleman, 20); // overwrite 6,7 where 6 is a gap

    assertThatTracker(target).matches(
        snapshot().gap(5).part(1, source, 105).gap(1).part(1, source2, 200).part(2, source, 108));
  }

  @Test public void testOverwriteWithGapInTheEnd() {
    // middleman has known source at the beginning, but not entirely at where we're reading from it;
    // we'll be reading from a known part + gap of middleman
    middleman.setSource(0, 21, source2, 200);

    target.setSource(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSource(6, 2, middleman, 20); // overwrite 6,7 where 7 is a gap

    assertThatTracker(target).matches(
        snapshot().gap(5).part(1, source, 105).part(1, source2, 220).gap(1).part(2, source, 108));
  }

  @Test public void testOverwriteWithZeroLength() {
    target.setSource(5, 5, source, 105);
    target.setSource(7, 0, source2, 200);

    assertThatTracker(target).matches(snapshot().gap(5).part(5, source, 105));
  }

  @Test public void testOverwriteWithZeroLengthUntracked() {
    target.setSource(5, 5, source, 105);
    target.setSource(7, 0, null, 200);

    assertThatTracker(target).matches(snapshot().gap(5).part(5, source, 105));
  }

  /** Use the source of the source if the direct source is mutable */
  @Test public void testMutableMiddleman() {
    middleman.setSource(0, 10, source, 0);
    target.setSource(0, 10, middleman, 0);
    assertThatTracker(target).matches(snapshot().part(10, source, 0));

    // Even after changing middleman to another source,
    middleman.setSource(0, 10, source2, 0);
    // target still knows it came from first source.
    assertThatTracker(target).matches(snapshot().part(10, source, 0));
  }

  /**
   * Get the source of the source composed of two parts,
   * but only partly: dropping the begin and ending
   */
  @Test public void testTransitiveCombinePartParts() {
    // set middleman to 100,101,102,103,104,200,201,202,203
    middleman.setSource(0, 5, source, 100);
    middleman.setSource(5, 4, source2, 200);
    // set target to 101,102,103,104,200,201,202
    target.setSource(0, 7, middleman, 1);

    assertThatTracker(target).matches(snapshot().part(4, source, 101).part(3, source2, 200));
  }

  @Test public void testTransitiveEndUnknown() {
    // set middleman to 100,101,102
    middleman.setSource(0, 3, source, 100);
    // set target to unknown,100,101,102,unknown,unknown
    target.setSource(1, 5, middleman, 0);

    assertThatTracker(target).matches(snapshot().gap(1).part(3, source, 100));
  }

  @Test public void testTransitiveStartUnknown() {
    // set middleman to unknown,unknown,100,101,102
    middleman.setSource(2, 3, source, 100);
    // set target to unknown,unknown,unknown,100,101
    target.setSource(1, 4, middleman, 0);

    assertThatTracker(target).matches(snapshot().gap(3).part(2, source, 100));
  }

  /** Take only a part of a part of the source of the source */
  @Test public void testTransitiveSinglePartlyPart() {
    // set middleman to 100,101,102,103,104,105,106,107,108,109,110,111,112
    middleman.setSource(0, 12, source, 100);
    // set target to 102,103,104,105
    target.setSource(1000, 4, middleman, 2);

    assertThatTracker(target).matches(snapshot().gap(1000).part(4, source, 102));
  }

  /** Make sure entries existing before and after the one involved don't break things */
  @Test public void testTransitiveIgnoreBeforeAndAfter() {
    middleman.setSource(0, 1, source, 100);
    middleman.setSource(1, 1, source, 200);
    middleman.setSource(2, 1, source, 300);
    middleman.setSource(3, 1, source, 400);
    middleman.setSource(4, 1, source, 500);

    //set target to 200,300,400
    target.setSource(1000, 3, middleman, 1);

    assertThatTracker(target).matches(
        snapshot().gap(1000).part(1, source, 200).part(1, source, 300).part(1, source, 400));
  }

  // test that it correctly identifies which parts of `middleman` are relevant,
  // taking Growth into account
  @Test public void testGrowthParts() {
    middleman.setSource(0, 1, source, 7, Growth.NONE); // should be ignored
    middleman.setSource(1, 1, source, 100, Growth.NONE);
    middleman.setSource(2, 1, source, 200, Growth.NONE);
    middleman.setSource(3, 1, source, 300, Growth.NONE); // should be ignored

    target.setSource(50, 4, middleman, 1, Growth.DOUBLE);

    assertThatTracker(target).matches(snapshot()
        .gap(50)
        .part(2, source, 100, Growth.DOUBLE)
        .part(2, source, 200, Growth.DOUBLE));
  }

  // test that it correctly identifies which parts of `middleman` are relevant,
  // taking Growth into account
  @Test public void testGrowthPartsHalf() {
    middleman.setSource(0, 2, source, 7, Growth.NONE); // should be ignored
    middleman.setSource(2, 2, source, 200, Growth.NONE);
    middleman.setSource(4, 2, source, 400, Growth.NONE);
    middleman.setSource(6, 2, source, 600, Growth.NONE); // should be ignored

    target.setSource(50, 2, middleman, 2, Growth.HALF);

    assertThatTracker(target).matches(snapshot()
        .gap(50)
        .part(1, source, 200, Growth.HALF)
        .part(1, source, 400, Growth.HALF));

    // TODO test similar when it's not aligned
  }

  @Test public void testGrowthCutParts() {
    middleman.setSource(0, 10, source, 0, Growth.NONE);
    middleman.setSource(10, 20, source2, 0, Growth.NONE);
    target.setSource(0, 6, middleman, 8, Growth.DOUBLE);
    assertThatTracker(target).matches(snapshot()
        .part(4, source, 8, Growth.DOUBLE)
        .part(2, source2, 0, Growth.DOUBLE));
  }

  @Test public void testGrowthCombining() {
    middleman.setSource(100, 10000, source, 5, Growth.DOUBLE);
    target.setSource(1000, 30, middleman, 120, Growth.of(3, 1));
    assertThatTracker(target).matches(snapshot()
        .gap(1000)
        .part(30, source, 5 + (120-100)/2, Growth.of(6, 1)));
  }

  @Test public void testTransitiveGrowthTruncation() {
    middleman.setSource(0, 10, source, 0);
    // this is actually incorrect API usage: length (3) is not multiple of Growth.targetBlock (2).
    // so maybe this behaviour doesn't need to be preserved.
    target.setSource(0, 3, middleman, 0, Growth.DOUBLE);
    assertThatTracker(target).matches(snapshot()
        .part(2, source, 0, Growth.DOUBLE)
        .part(1, source, 1, Growth.NONE));
  }

  @Test public void testGrowthMisalignedStart() {
    // blocks in middleman start at 8, 18, 28,...
    middleman.setSource(8, 100, source, 1000, Growth.of(10, 1));
    // starting at 25, length 23 => 3 of the 18-28 block, and then 2 blocks of 10
    target.setSource(50, 23, middleman, 25, Growth.NONE);
    assertThatTracker(target).matches(snapshot()
        .gap(50)
        .part(3, source, 1001, Growth.of(3, 1))
        .part(20, source, 1002, Growth.of(10, 1)));
  }

  @Test public void testGrowthMisalignedEnd() {
    // blocks in middleman start at 8, 18, 28,...
    middleman.setSource(8, 100, source, 1000, Growth.of(10, 1));
    // starting at 28, length 23 => 2 blocks of 10 and then 3 of the 48-58 block
    target.setSource(50, 23, middleman, 28, Growth.NONE);
    assertThatTracker(target).matches(snapshot()
        .gap(50)
        .part(20, source, 1002, Growth.of(10, 1))
        .part(3, source, 1004, Growth.of(3, 1)));
  }

  /** Combination of {@link #testGrowthMisalignedStart()} and {@link #testGrowthMisalignedEnd()} */
  @Test public void testGrowthMisalignedStartAndEnd() {
    // blocks in middleman start at 8, 18, 28,...
    middleman.setSource(8, 100, source, 1000, Growth.of(10, 1));
    // starting at 25, length 25 => 3 of the 18-28 block, and then 2 blocks of 10, then 2 more
    target.setSource(50, 25, middleman, 25, Growth.NONE);
    assertThatTracker(target).matches(snapshot()
        .gap(50)
        .part(3, source, 1001, Growth.of(3, 1))
        .part(20, source, 1002, Growth.of(10, 1))
        .part(2, source, 1004, Growth.of(2, 1)));
  }

  /**
   * Different kind of combination of {@link #testGrowthMisalignedStart()} and
   * {@link #testGrowthMisalignedEnd()} than {@link #testGrowthMisalignedStartAndEnd()}
   */
  @Test public void testGrowthMisalignedStartAndEndSmall() {
    // blocks in middleman start at 8, 18, 28,...
    middleman.setSource(8, 100, source, 1000, Growth.of(10, 1));
    // starting at 25, length 2 => one piece of 2, that doesn't align at start nor end
    target.setSource(50, 2, middleman, 25, Growth.NONE);
    assertThatTracker(target).matches(snapshot()
        .gap(50)
        .part(2, source, 1001, Growth.of(2, 1)));
  }

  @Test public void testOverwriteSelfBackwards() {
    target.setSource(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSource(6, 2, target, 8);
    assertThatTracker(target).matches(
        snapshot().gap(5).part(1, source, 105).part(2, source, 108).part(2, source, 108));
  }

  @Test public void testOverwriteSelfForwards() {
    target.setSource(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSource(6, 2, target, 5);

    assertThatTracker(target).matches(
        snapshot().gap(5).part(1, source, 105).part(2, source, 105).part(2, source, 108));
  }
}
