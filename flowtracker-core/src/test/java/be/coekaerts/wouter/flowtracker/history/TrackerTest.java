package be.coekaerts.wouter.flowtracker.history;

import be.coekaerts.wouter.flowtracker.tracker.DefaultTracker;
import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import org.junit.Before;
import org.junit.Test;

import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;

public class TrackerTest {
  private Tracker source, source2, target, middleman;

  @Before public void setupSource() {
    source = new FixedOriginTracker(1000);
    source2 = new FixedOriginTracker(1000);
    target = new DefaultTracker();
    middleman = new DefaultTracker();
  }

  @Test public void testSetSingleSource() {
    target.setSourceFromTracker(5, 3, source, 105); // setting 5,6,7

    snapshotBuilder().gap(5).part(source, 105, 3)
        .assertEquals(target);
  }

  /** Set one source, then a second one after it, leaving a gap in between */
  @Test public void setSecondSourceAfterGap() {
    target.setSourceFromTracker(5, 3, source, 105); // setting 5,6,7
    target.setSourceFromTracker(9, 2, source, 109); // setting 9,10, leaving a gap at 8

    snapshotBuilder().gap(5).part(source, 105, 3).gap(1).part(source, 109, 2)
        .assertEquals(target);
  }

  /** Set one source, then a second one before it, leaving a gap in between */
  @Test public void setSecondSourceBeforeGap() {
    target.setSourceFromTracker(5, 3, source, 105); // setting 5,6,7
    target.setSourceFromTracker(2, 2, source, 102); // setting 2,3, leaving a gap at 4

    snapshotBuilder().gap(2).part(source, 102, 2).gap(1).part(source, 105, 3)
        .assertEquals(target);
  }

  /** Merge an entry if it comes after an existing one that matches */
  @Test public void testTouchMergeWithPrevious() {
    target.setSourceFromTracker(5, 3, source, 105); // setting 5,6,7
    target.setSourceFromTracker(8, 2, source, 108); // setting 8,9

    snapshotBuilder().gap(5).part(source, 105, 5)
        .assertEquals(target);
  }

  /** Don't merge an entry if it comes right after one, but the indices don't match */
  @Test public void testTouchDontMergeWithPreviousWhenSkipped() {
    target.setSourceFromTracker(5, 3, source, 105); // setting 5,6,7 to 105,...
    target.setSourceFromTracker(8, 2, source, 109); // setting 8,9 to 109,... (skipping 108)

    snapshotBuilder().gap(5).part(source, 105, 3).part(source, 109, 2)
        .assertEquals(target);
  }

  /** Merge an entry if it comes before an existing one that matches */
  @Test public void testTouchMergeWithNext() {
    target.setSourceFromTracker(5, 3, source, 105); // setting 5,6,7
    target.setSourceFromTracker(3, 2, source, 103); // setting 3,4

    snapshotBuilder().gap(3).part(source, 103, 5)
        .assertEquals(target);
  }

  /** Don't merge an entry if it comes right before one, but the indices don't match */
  @Test public void testTouchDontMergeWithNextWhenSkipped() {
    target.setSourceFromTracker(5, 3, source, 105); // setting 5,6,7
    target.setSourceFromTracker(3, 2, source, 102); // setting 3,4

    snapshotBuilder().gap(3).part(source, 102, 2).part(source, 105, 3)
        .assertEquals(target);
  }

  /** Don't merge entries with a different source */
  @Test public void testDontMergeDifferentSource() {
    target.setSourceFromTracker(5, 3, source, 105); // setting 5,6,7
    target.setSourceFromTracker(8, 2, source2, 108); // setting 8,9

    snapshotBuilder().gap(5).part(source, 105, 3).part(source2, 108, 2)
        .assertEquals(target);
  }

  /** Insert the missing part in between two entries with a hole, expect the three to merge */
  @Test public void testTouchMergeWithPreviousAndNext() {
    target.setSourceFromTracker(10, 2, source, 110); // setting 10,11
    target.setSourceFromTracker(5, 3, source, 105); // setting 5,6,7
    target.setSourceFromTracker(8, 2, source, 108); // setting 8,9

    snapshotBuilder().gap(5).part(source, 105, 7)
        .assertEquals(target);
  }

  @Test public void testOverlapMergeWithPrevious() {
    target.setSourceFromTracker(5, 3, source, 105); // setting 5,6,7
    target.setSourceFromTracker(6, 4, source, 106); // setting 6,7,8,9

    snapshotBuilder().gap(5).part(source, 105, 5)
        .assertEquals(target);
  }

  @Test public void testOverlapMergeWithNext() {
    target.setSourceFromTracker(6, 4, source, 106); // setting 6,7,8,9
    target.setSourceFromTracker(5, 3, source, 105); // setting 5,6,7

    snapshotBuilder().gap(5).part(source, 105, 5)
        .assertEquals(target);
  }

  @Test public void testOverlapOverwritePrevious() {
    target.setSourceFromTracker(5, 3, source, 105); // setting 5,6,7
    target.setSourceFromTracker(6, 4, source2, 106); // setting 6,7,8,9

    snapshotBuilder().gap(5).part(source, 105, 1).part(source2, 106, 4)
        .assertEquals(target);
  }

  @Test public void testOverlapOverwriteNext() {
    target.setSourceFromTracker(6, 4, source, 106); // setting 6,7,8,9
    target.setSourceFromTracker(5, 3, source2, 105); // setting 5,6,7

    snapshotBuilder().gap(5).part(source2, 105, 3).part(source, 108, 2)
        .assertEquals(target);
  }

  @Test public void testOverlapOverwriteCompletely() {
    target.setSourceFromTracker(5, 3, source, 105); // setting 5,6,7
    target.setSourceFromTracker(4, 5, source2, 104); // setting 4,5,6,7,8

    snapshotBuilder().gap(4).part(source2, 104, 5)
        .assertEquals(target);
  }

  @Test public void testOverwriteSameRange() {
    target.setSourceFromTracker(5, 3, source, 105); // setting 5,6,7
    target.setSourceFromTracker(5, 3, source2, 105); // setting 5,6,7

    snapshotBuilder().gap(5).part(source2, 105, 3)
        .assertEquals(target);
  }

  @Test public void testOverlapOverwriteMultiple() {
    target.setSourceFromTracker(5, 3, source, 10);
    target.setSourceFromTracker(8, 3, source, 20);
    target.setSourceFromTracker(11, 3, source, 30);
    target.setSourceFromTracker(14, 3, source, 40);
    target.setSourceFromTracker(6, 10, source, 100);

    snapshotBuilder().gap(5).part(source, 10, 1).part(source, 100, 10).part(source, 42, 1)
        .assertEquals(target);
  }

  @Test public void testOverwriteMiddle() {
    target.setSourceFromTracker(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSourceFromTracker(6, 2, source2, 106); // setting 6,7

    snapshotBuilder().gap(5).part(source, 105, 1).part(source2, 106, 2).part(source, 108, 2)
        .assertEquals(target);
  }

  @Test public void testOverwriteStart() {
    target.setSourceFromTracker(5, 5, source, 105);
    target.setSourceFromTracker(5, 1, source2, 105);

    snapshotBuilder().gap(5).part(source2, 105, 1).part(source, 106, 4)
        .assertEquals(target);
  }

  @Test public void testOverwriteEnd() {
    target.setSourceFromTracker(5, 5, source, 105);
    target.setSourceFromTracker(9, 1, source2, 109);

    snapshotBuilder().gap(5).part(source, 105, 4).part(source2, 109, 1)
        .assertEquals(target);
  }

  @Test public void testOverwriteWithSame() {
    target.setSourceFromTracker(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSourceFromTracker(6, 2, source, 106); // setting 6,7 to the same again

    snapshotBuilder().gap(5).part(source, 105, 5)
        .assertEquals(target);
  }

  @Test public void testOverwriteWithUntracked() {
    target.setSourceFromTracker(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSourceFromTracker(6, 2, null, 999); // overwrite 6,7 with untracked object

    snapshotBuilder().gap(5).part(source, 105, 1).gap(2).part(source, 108, 2)
        .assertEquals(target);
  }

  @Test public void testOverwriteWithGap() {
    // middleman has known source at start and end, but not at where we're reading from it;
    // in other words we'll be reading from a gap in the middle of middleman
    middleman.setSourceFromTracker(0, 1, source2, 0);
    middleman.setSourceFromTracker(50, 1, source2, 0);

    target.setSourceFromTracker(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSourceFromTracker(6, 2, middleman, 20); // overwrite 6,7 with gap

    snapshotBuilder().gap(5).part(source, 105, 1).gap(2).part(source, 108, 2)
        .assertEquals(target);
  }

  @Test public void testOverwriteWithGapInTheMiddle() {
    // middleman has known source at start and end, but not entirely at where we're reading from it;
    // we'll be reading from a known part + gap + known part of middleman
    middleman.setSourceFromTracker(0, 21, source2, 200);
    middleman.setSourceFromTracker(22, 1000, source2, 300);

    target.setSourceFromTracker(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSourceFromTracker(6, 3, middleman, 20); // overwrite 6,7,8 where 7 is the gap

    snapshotBuilder().gap(5)
        .part(source, 105, 1)
        .part(source2, 220, 1)
        .gap(1)
        .part(source2, 300, 1)
        .part(source, 109, 1)
        .assertEquals(target);
  }

  @Test public void testOverwriteWithGapInTheBeginning() {
    // middleman has known source at the end, but not entirely at where we're reading from it;
    // we'll be reading from a gap + known part of middleman
    middleman.setSourceFromTracker(21, 1000, source2, 200);

    target.setSourceFromTracker(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSourceFromTracker(6, 2, middleman, 20); // overwrite 6,7 where 6 is a gap

    snapshotBuilder().gap(5).part(source, 105, 1).gap(1).part(source2, 200, 1).part(source, 108, 2)
        .assertEquals(target);
  }

  @Test public void testOverwriteWithGapInTheEnd() {
    // middleman has known source at the beginning, but not entirely at where we're reading from it;
    // we'll be reading from a known part + gap of middleman
    middleman.setSourceFromTracker(0, 21, source2, 200);

    target.setSourceFromTracker(5, 5, source, 105); // setting 5,6,7,8,9
    target.setSourceFromTracker(6, 2, middleman, 20); // overwrite 6,7 where 7 is a gap

    snapshotBuilder().gap(5).part(source, 105, 1).part(source2, 220, 1).gap(1).part(source, 108, 2)
        .assertEquals(target);
  }

  @Test public void testOverwriteWithZeroLength() {
    target.setSourceFromTracker(5, 5, source, 105);
    target.setSourceFromTracker(7, 0, source2, 200);

    snapshotBuilder().gap(5).part(source, 105, 5)
        .assertEquals(target);
  }

  @Test public void testOverwriteWithZeroLengthUntracked() {
    target.setSourceFromTracker(5, 5, source, 105);
    target.setSourceFromTracker(7, 0, null, 200);

    snapshotBuilder().gap(5).part(source, 105, 5)
        .assertEquals(target);
  }

  /** Use the source of the source if the direct source is mutable */
  @Test public void testMutableMiddleman() {
    middleman.setSourceFromTracker(0, 10, source, 0);
    target.setSourceFromTracker(0, 10, middleman, 0);
    snapshotBuilder().part(source, 0, 10)
        .assertEquals(target);

    // Even after changing middleman to another source,
    middleman.setSourceFromTracker(0, 10, source2, 0);
    // target still knows it came from first source.
    snapshotBuilder().part(source, 0, 10)
        .assertEquals(target);
  }

  /**
   * Get the source of the source composed of two parts,
   * but only partly: dropping the begin and ending
   */
  @Test public void testTransitiveCombinePartParts() {
    // set middleman to 100,101,102,103,104,200,201,202,203
    middleman.setSourceFromTracker(0, 5, source, 100);
    middleman.setSourceFromTracker(5, 4, source2, 200);
    // set target to 101,102,103,104,200,201,202
    target.setSourceFromTracker(0, 7, middleman, 1);

    snapshotBuilder().part(source, 101, 4).part(source2, 200, 3)
        .assertEquals(target);
  }

  @Test public void testTransitiveEndUnknown() {
    // set middleman to 100,101,102
    middleman.setSourceFromTracker(0, 3, source, 100);
    // set target to unknown,100,101,102,unknown,unknown
    target.setSourceFromTracker(1, 5, middleman, 0);

    snapshotBuilder().gap(1).part(source, 100, 3)
        .assertEquals(target);
  }

  @Test public void testTransitiveStartUnknown() {
    // set middleman to unknown,unknown,100,101,102
    middleman.setSourceFromTracker(2, 3, source, 100);
    // set target to unknown,unknown,unknown,100,101
    target.setSourceFromTracker(1, 4, middleman, 0);

    snapshotBuilder().gap(3).part(source, 100, 2)
        .assertEquals(target);
  }

  /** Take only a part of a part of the source of the source */
  @Test public void testTransitiveSinglePartlyPart() {
    // set middleman to 100,101,102,103,104,105,106,107,108,109,110,111,112
    middleman.setSourceFromTracker(0, 12, source, 100);
    // set target to 102,103,104,105
    target.setSourceFromTracker(1000, 4, middleman, 2);

    snapshotBuilder().gap(1000).part(source, 102, 4)
        .assertEquals(target);
  }

  /** Make sure entries existing before and after the one involved don't break things */
  @Test public void testTransitiveIgnoreBeforeAndAfter() {
    middleman.setSourceFromTracker(0, 1, source, 100);
    middleman.setSourceFromTracker(1, 1, source, 200);
    middleman.setSourceFromTracker(2, 1, source, 300);
    middleman.setSourceFromTracker(3, 1, source, 400);
    middleman.setSourceFromTracker(4, 1, source, 500);

    //set target to 200,300,400
    target.setSourceFromTracker(1000, 3, middleman, 1);

    snapshotBuilder().gap(1000).part(source, 200, 1).part(source, 300, 1).part(source, 400, 1)
        .assertEquals(target);
  }
}
