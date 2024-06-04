package com.coekie.flowtracker.web;

import static com.coekie.flowtracker.web.TrackerResource.TrackerDetailResponse;
import static com.google.common.truth.Truth.assertThat;

import com.coekie.flowtracker.tracker.ByteOriginTracker;
import com.coekie.flowtracker.tracker.ByteSinkTracker;
import com.coekie.flowtracker.tracker.CharOriginTracker;
import com.coekie.flowtracker.tracker.CharSinkTracker;
import com.coekie.flowtracker.tracker.ClassOriginTracker;
import com.coekie.flowtracker.tracker.FixedOriginTracker;
import com.coekie.flowtracker.tracker.Growth;
import com.coekie.flowtracker.tracker.Tracker;
import com.coekie.flowtracker.tracker.TrackerTree;
import com.coekie.flowtracker.tracker.TrackerTree.Node;
import com.coekie.flowtracker.web.TrackerResource.Region;
import org.junit.Before;
import org.junit.Test;

public class TrackerResourceTest {
  private TrackerResource trackerResource;

  @Before public void setup() {
    trackerResource = new TrackerResource();
  }

  @Test public void getCharSinkTracker() {
    CharSinkTracker tracker = new CharSinkTracker();
    InterestRepository.register(tracker);

    Tracker sourceTracker1 = new FixedOriginTracker(3);
    Tracker sourceTracker2 = new FixedOriginTracker(3);

    // content: abcdefgh
    // index:   0123456
    // source1:   cde
    // source2:     xxgxx

    tracker.append("ab", 0, 2); // gap, no source tracker
    tracker.append("cde", 0, 3);
    tracker.setSource(2, 3, sourceTracker1, 0);
    tracker.append('f'); // gap, no source tracker
    tracker.append("xxgxx", 2, 1);
    tracker.setSource(6, 1, sourceTracker2, 2);
    tracker.append('h');

    TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
    assertThat(response.regions.size()).isEqualTo(5);

    assertRegionNoPart(response.regions.get(0), "ab");
    assertRegionOnePart(response.regions.get(1), "cde", sourceTracker1, 0);
    assertRegionNoPart(response.regions.get(2), "f");
    assertRegionOnePart(response.regions.get(3), "g", sourceTracker2, 2);
    assertRegionNoPart(response.regions.get(4), "h");
  }

  @Test public void getByteSinkTracker() {
    ByteSinkTracker tracker = new ByteSinkTracker();
    InterestRepository.register(tracker);

    Tracker sourceTracker1 = new FixedOriginTracker(3);
    Tracker sourceTracker2 = new FixedOriginTracker(3);

    // content: abcdefgh
    // index:   0123456
    // source1:   cde
    // source2:     xxgxx

    tracker.append("ab".getBytes(), 0, 2); // gap, no source tracker
    tracker.append("cde".getBytes(), 0, 3);
    tracker.setSource(2, 3, sourceTracker1, 0);
    tracker.append((byte) 'f'); // gap, no source tracker
    tracker.append("xxgxx".getBytes(), 2, 1);
    tracker.setSource(6, 1, sourceTracker2, 2);
    tracker.append((byte) 'h');

    TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
    assertThat(response.regions).hasSize(5);

    assertRegionNoPart(response.regions.get(0), "ab");
    assertRegionOnePart(response.regions.get(1), "cde", sourceTracker1, 0);
    assertRegionNoPart(response.regions.get(2), "f");
    assertRegionOnePart(response.regions.get(3), "g", sourceTracker2, 2);
    assertRegionNoPart(response.regions.get(4), "h");
  }

  @Test public void getCharOriginTracker() {
    CharOriginTracker tracker = new CharOriginTracker();
    InterestRepository.register(tracker);

    tracker.append("abc");

    TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
    assertThat(response.regions).hasSize(1);

    assertRegionNoPart(response.regions.get(0), "abc");
  }

  @Test public void getByteOriginTracker() {
    ByteOriginTracker tracker = new ByteOriginTracker();
    InterestRepository.register(tracker);

    tracker.append("abc".getBytes(), 0, 3);

    TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
    assertThat(response.regions).hasSize(1);

    assertRegionNoPart(response.regions.get(0), "abc");
  }

  @Test public void escapeBytes() {
    ByteOriginTracker tracker = new ByteOriginTracker();
    InterestRepository.register(tracker);

    tracker.append("abc".getBytes(), 0, 3);
    tracker.append((byte) 1);
    tracker.append((byte) 2);
    tracker.append((byte) 255);
    tracker.append("def\r\n".getBytes(), 0, 5);

    TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
    assertThat(response.regions).hasSize(1);

    assertRegionNoPart(response.regions.get(0), "abc❲01 02 FF❳def\r\n");
  }

  @Test public void growth() {
    ByteSinkTracker tracker = new ByteSinkTracker();
    InterestRepository.register(tracker);

    Tracker sourceTracker = new FixedOriginTracker(3);

    tracker.append("ab".getBytes(), 0, 2); // gap, no source tracker
    tracker.append("cde".getBytes(), 0, 3);
    tracker.setSource(2, 3, sourceTracker, 0, Growth.of(1, 4));

    TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
    assertThat(response.regions).hasSize(2);

    assertRegionNoPart(response.regions.get(0), "ab");
    assertRegionOnePart(response.regions.get(1), "cde", sourceTracker, 0, 12);
  }

  @Test public void reverse() {
    CharSinkTracker target = new CharSinkTracker();
    CharOriginTracker source = new CharOriginTracker();
    InterestRepository.register(target);
    InterestRepository.register(source);

    source.append("xabcdex");
    target.append("..abc.cde.", 0, 9);
    target.setSource(2, 3, source, 1);
    target.setSource(6, 3, source, 3);

    TrackerDetailResponse response = trackerResource.reverse(source.getTrackerId(),
        target.getTrackerId());
    assertThat(response.regions).hasSize(5);

    assertRegionNoPart(response.regions.get(0), "x");
    assertRegionOnePart(response.regions.get(1), "ab", target, 2, 3);

    // the region with two parts
    Region region = response.regions.get(2);
    assertThat(region.content).isEqualTo("c");
    assertThat(region.parts).hasSize(2);
    assertThat(region.parts.get(0).tracker.id).isEqualTo(target.getTrackerId());
    // note that this points to the *beginning* of the part (index 2), which does not correspond to
    // the beginning of this region.
    assertThat(region.parts.get(0).offset).isEqualTo(2);
    assertThat(region.parts.get(1).tracker.id).isEqualTo(target.getTrackerId());
    assertThat(region.parts.get(1).offset).isEqualTo(6);

    assertRegionOnePart(response.regions.get(3), "de", target, 6, 3);
    assertRegionNoPart(response.regions.get(4), "x");
  }

  @Test public void reverseGrowth() {
    CharSinkTracker target = new CharSinkTracker();
    CharOriginTracker source = new CharOriginTracker();
    InterestRepository.register(target);
    InterestRepository.register(source);

    source.append("xabcx");
    target.append(".....ss.....", 0, 9);
    target.setSource(5, 2, source, 1, Growth.of(2, 3));

    TrackerDetailResponse response = trackerResource.reverse(source.getTrackerId(),
        target.getTrackerId());
    assertThat(response.regions).hasSize(3);

    assertRegionNoPart(response.regions.get(0), "x");
    assertRegionOnePart(response.regions.get(1), "abc", target, 5, 2);
  }

  /**
   * Test reverse with a region that spans from start to end. Regression test for an off-by-one-ish
   * problem.
   */
  @Test public void reverseOneRegion() {
    CharSinkTracker target = new CharSinkTracker();
    CharOriginTracker source = new CharOriginTracker();
    InterestRepository.register(target);
    InterestRepository.register(source);

    source.append("bc");
    target.append("abcd", 0, 4);
    target.setSource(1, 2, source, 0);

    TrackerDetailResponse response = trackerResource.reverse(source.getTrackerId(),
        target.getTrackerId());
    assertThat(response.regions).hasSize(1);
    assertRegionOnePart(response.regions.get(0), "bc", target, 1);
  }

  @Test public void reverseClassOriginTrackerSource() {
    CharSinkTracker target = new CharSinkTracker();
    ClassOriginTracker source = ClassOriginTracker.registerClass(null, "myClass", null);
    InterestRepository.register(target);
    InterestRepository.register(source);

    source.startMethod("testing");
    source.registerConstantString("hello", 7);

    TrackerDetailResponse response = trackerResource.reverse(source.getTrackerId(),
        target.getTrackerId());
    assertThat(response.regions).hasSize(3);

    assertRegionNoPart(response.regions.get(0), "class myClass\n"
        + "testing:\n"
        + "  (line 7) ");
    assertRegionNoPart(response.regions.get(1), "hello");
    assertRegionNoPart(response.regions.get(2), "\n");

    assertThat(response.regions.get(0).line).isNull();
    assertThat(response.regions.get(1).line).isEqualTo(7);
    assertThat(response.regions.get(2).line).isNull();
  }

  @Test public void path() {
    Node root = TrackerTree.node("TrackerResourceTest.test");
    Node one = root.node("a");
    Node a = one.node("b");
    Tracker tracker = new ByteOriginTracker().addTo(a);
    InterestRepository.register(tracker);
    TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
    assertThat(response.path).containsExactly("TrackerResourceTest.test", "a", "b").inOrder();
  }

  @Test public void creationStackTrace() {
    boolean oldCreationStackTraceEnabled = Tracker.trackCreation;
    Tracker.trackCreation = true;
    try {
      Node node = TrackerTree.node("TrackerResourceTest.creationStackTrace");
      Tracker tracker = new ByteOriginTracker().addTo(node);
      InterestRepository.register(tracker);
      TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
      assertThat(response.creationStackTrace).contains("TrackerResourceTest");
    } finally {
      Tracker.trackCreation = oldCreationStackTraceEnabled;
    }
  }

  @Test public void creationStackTraceDisabled() {
    boolean oldCreationStackTraceEnabled = Tracker.trackCreation;
    Tracker.trackCreation = false;
    try {
      Node node = TrackerTree.node("TrackerResourceTest.creationStackTrace");
      Tracker tracker = new ByteOriginTracker().addTo(node);
      InterestRepository.register(tracker);
      TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
      assertThat(response.creationStackTrace).isNull();
    } finally {
      Tracker.trackCreation = oldCreationStackTraceEnabled;
    }
  }

  private void assertRegionNoPart(Region region, String expectedContent) {
    assertThat(region.content).isEqualTo(expectedContent);
    assertThat(region.parts).isEmpty();
  }

  private void assertRegionOnePart(Region region, String expectedContent, Tracker expectedTracker,
      int expectedOffset) {
    assertRegionOnePart(region, expectedContent, expectedTracker, expectedOffset,
        expectedContent.length());
  }

  private void assertRegionOnePart(Region region, String expectedContent, Tracker expectedTracker,
      int expectedOffset, int length) {
    assertThat(region.content).isEqualTo(expectedContent);
    assertThat(region.parts).hasSize(1);
    assertThat(region.parts.get(0).tracker.id).isEqualTo(expectedTracker.getTrackerId());
    assertThat(region.parts.get(0).offset).isEqualTo(expectedOffset);
    assertThat(region.parts.get(0).length).isEqualTo(length);
  }
}
