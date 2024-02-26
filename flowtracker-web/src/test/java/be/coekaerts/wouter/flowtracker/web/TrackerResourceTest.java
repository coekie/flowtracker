package be.coekaerts.wouter.flowtracker.web;

import static be.coekaerts.wouter.flowtracker.web.TrackerResource.TrackerDetailResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import be.coekaerts.wouter.flowtracker.tracker.ByteSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.CharOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.CharSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.web.TrackerResource.Region;
import org.junit.Before;
import org.junit.Test;

public class TrackerResourceTest {
  private TrackerResource trackerResource;

  @Before public void setup() {
    trackerResource = new TrackerResource();
  }

  @Test public void getCharSinkTracker() {
    CharSinkTracker tracker = new CharSinkTracker();
    TrackerRepository.setTracker(new Object(), tracker);
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
    assertEquals(5, response.getRegions().size());

    assertRegionNoPart(response.getRegions().get(0), "ab");
    assertRegionOnePart(response.getRegions().get(1), "cde", sourceTracker1, 0);
    assertRegionNoPart(response.getRegions().get(2), "f");
    assertRegionOnePart(response.getRegions().get(3), "g", sourceTracker2, 2);
    assertRegionNoPart(response.getRegions().get(4), "h");
  }

  @Test public void getByteSinkTracker() {
    ByteSinkTracker tracker = new ByteSinkTracker();
    TrackerRepository.setTracker(new Object(), tracker);
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
    assertEquals(5, response.getRegions().size());

    assertRegionNoPart(response.getRegions().get(0), "ab");
    assertRegionOnePart(response.getRegions().get(1), "cde", sourceTracker1, 0);
    assertRegionNoPart(response.getRegions().get(2), "f");
    assertRegionOnePart(response.getRegions().get(3), "g", sourceTracker2, 2);
    assertRegionNoPart(response.getRegions().get(4), "h");
  }

  @Test public void trackerPartResponse_sourceContext() {
    CharSinkTracker tracker = new CharSinkTracker();
    TrackerRepository.setTracker(new Object(), tracker);
    InterestRepository.register(tracker);

    CharOriginTracker sourceTracker = new CharOriginTracker();
    sourceTracker.append("abcdefghijklmnopqrstuvwxyz".toCharArray(), 0, 26);
    tracker.append("mnbcxy", 0, 6);
    tracker.setSource(0, 2, sourceTracker, 12);
    tracker.setSource(2, 2, sourceTracker, 1);
    tracker.setSource(4, 2, sourceTracker, 24);

    TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
    // middle, full context
    assertEquals("cdefghijklmnopqrstuvwx",
        response.getRegions().get(0).getParts().get(0).getContext());
    // context near the beginning
    assertEquals("abcdefghijklm", response.getRegions().get(1).getParts().get(0).getContext());
    // context near the end
    assertEquals("opqrstuvwxyz", response.getRegions().get(2).getParts().get(0).getContext());
  }

  @Test public void getOriginTracker() {
    CharOriginTracker tracker = new CharOriginTracker();
    TrackerRepository.setTracker(new Object(), tracker);
    InterestRepository.register(tracker);

    tracker.append(new char[] {'a', 'b', 'c', 'd'}, 1, 2);

    TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
    assertEquals(1, response.getRegions().size());

    assertRegionNoPart(response.getRegions().get(0), "bc");
  }

  private void assertRegionNoPart(Region region, String expectedContent) {
    assertEquals(expectedContent, region.getContent());
    assertTrue(region.getParts().isEmpty());
  }

  private void assertRegionOnePart(Region region, String expectedContent, Tracker expectedTracker,
      int expectedOffset) {
    assertEquals(expectedContent, region.getContent());
    assertEquals(1, region.getParts().size());
    assertEquals(expectedTracker.getTrackerId(), region.getParts().get(0).getTracker().getId());
    assertEquals(expectedOffset, region.getParts().get(0).getOffset());
  }
}
