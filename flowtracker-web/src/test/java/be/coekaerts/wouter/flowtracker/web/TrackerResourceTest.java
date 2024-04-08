package be.coekaerts.wouter.flowtracker.web;

import static be.coekaerts.wouter.flowtracker.web.TrackerResource.TrackerDetailResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.tracker.ByteOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.CharOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.CharSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree;
import be.coekaerts.wouter.flowtracker.tracker.TrackerTree.Node;
import be.coekaerts.wouter.flowtracker.web.TrackerResource.Region;
import java.util.Arrays;
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
    assertEquals(5, response.regions.size());

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
    assertEquals(5, response.regions.size());

    assertRegionNoPart(response.regions.get(0), "ab");
    assertRegionOnePart(response.regions.get(1), "cde", sourceTracker1, 0);
    assertRegionNoPart(response.regions.get(2), "f");
    assertRegionOnePart(response.regions.get(3), "g", sourceTracker2, 2);
    assertRegionNoPart(response.regions.get(4), "h");
  }

  @Test public void getOriginTracker() {
    CharOriginTracker tracker = new CharOriginTracker();
    InterestRepository.register(tracker);

    tracker.append(new char[] {'a', 'b', 'c', 'd'}, 1, 2);

    TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
    assertEquals(1, response.regions.size());

    assertRegionNoPart(response.regions.get(0), "bc");
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
    assertEquals(5, response.regions.size());

    assertRegionNoPart(response.regions.get(0), "x");
    assertRegionOnePart(response.regions.get(1), "ab", target, 2);

    // the region with two parts
    Region region = response.regions.get(2);
    assertEquals("c", region.content);
    assertEquals(2, region.parts.size());
    assertEquals(target.getTrackerId(), region.parts.get(0).tracker.id);
    // note that this points to the *beginning* of the part (index 2), which does not correspond to
    // the beginning of this region.
    assertEquals(2, region.parts.get(0).offset);
    assertEquals(target.getTrackerId(), region.parts.get(1).tracker.id);
    assertEquals(6, region.parts.get(1).offset);

    assertRegionOnePart(response.regions.get(3), "de", target, 6);
    assertRegionNoPart(response.regions.get(4), "x");
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
    assertEquals(1, response.regions.size());
    assertRegionOnePart(response.regions.get(0), "bc", target, 1);
  }

  @Test public void path() {
    Node root = TrackerTree.node("TrackerResourceTest.test");
    Node one = root.node("a");
    Node a = one.node("b");
    Tracker tracker = new ByteOriginTracker().addTo(a);
    InterestRepository.register(tracker);
    TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
    assertEquals(Arrays.asList("TrackerResourceTest.test", "a", "b"), response.path);
  }

  @Test public void creationStackTrace() {
    boolean oldCreationStackTraceEnabled = Tracker.trackCreation;
    Tracker.trackCreation = true;
    try {
      Node node = TrackerTree.node("TrackerResourceTest.creationStackTrace");
      Tracker tracker = new ByteOriginTracker().addTo(node);
      InterestRepository.register(tracker);
      TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
      assertTrue(response.creationStackTrace.contains("TrackerResourceTest"));
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
      assertNull(response.creationStackTrace);
    } finally {
      Tracker.trackCreation = oldCreationStackTraceEnabled;
    }
  }

  private void assertRegionNoPart(Region region, String expectedContent) {
    assertEquals(expectedContent, region.content);
    assertTrue(region.parts.isEmpty());
  }

  private void assertRegionOnePart(Region region, String expectedContent, Tracker expectedTracker,
      int expectedOffset) {
    assertEquals(expectedContent, region.content);
    assertEquals(1, region.parts.size());
    assertEquals(expectedTracker.getTrackerId(), region.parts.get(0).tracker.id);
    assertEquals(expectedOffset, region.parts.get(0).offset);
  }
}
