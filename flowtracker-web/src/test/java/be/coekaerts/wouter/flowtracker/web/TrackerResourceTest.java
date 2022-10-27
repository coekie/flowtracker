package be.coekaerts.wouter.flowtracker.web;

import static be.coekaerts.wouter.flowtracker.web.TrackerResource.TrackerDetailResponse;
import static be.coekaerts.wouter.flowtracker.web.TrackerResource.TrackerResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import be.coekaerts.wouter.flowtracker.tracker.CharOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.CharSinkTracker;
import be.coekaerts.wouter.flowtracker.tracker.DefaultTracker;
import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.TagTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import org.junit.Before;
import org.junit.Test;

public class TrackerResourceTest {
  private TrackerResource trackerResource;

  @Before public void setup() {
    trackerResource = new TrackerResource();
  }

  @Test public void list() {
    Tracker tracker = new DefaultTracker();
    tracker.initDescriptor("myTracker", new TagTracker("descriptorTracker"));
    TrackerRepository.setInterestTracker(new Object(), tracker);

    for (TrackerResponse t : trackerResource.list()) {
      if (t.getId() == tracker.getTrackerId()) {
        assertEquals("myTracker from descriptorTracker", t.getDescription());
        return;
      }
    }
    fail("tracker with right id not returned");
  }

  @Test public void getSinkTracker() {
    CharSinkTracker tracker = new CharSinkTracker();
    TrackerRepository.setInterestTracker(new Object(), tracker);

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
    assertEquals(5, response.getParts().size());

    assertEquals("ab", response.getParts().get(0).getContent());
    assertNull(response.getParts().get(0).getSource());

    assertEquals("cde", response.getParts().get(1).getContent());
    assertEquals(sourceTracker1.getTrackerId(), response.getParts().get(1).getSource().getId());
    assertEquals(0, response.getParts().get(1).getSourceOffset());

    assertEquals("f", response.getParts().get(2).getContent());
    assertNull(response.getParts().get(2).getSource());

    assertEquals("g", response.getParts().get(3).getContent());
    assertEquals(sourceTracker2.getTrackerId(), response.getParts().get(3).getSource().getId());
    assertEquals(2, response.getParts().get(3).getSourceOffset());

    assertEquals("h", response.getParts().get(4).getContent());
    assertNull(response.getParts().get(4).getSource());
  }

  @Test public void trackerPartResponse_sourceContext() {
    CharSinkTracker tracker = new CharSinkTracker();
    TrackerRepository.setInterestTracker(new Object(), tracker);

    CharOriginTracker sourceTracker = new CharOriginTracker();
    sourceTracker.append("abcdefghijklmnopqrstuvwxyz".toCharArray(), 0, 26);
    tracker.append("mnbcxy", 0, 6);
    tracker.setSource(0, 2, sourceTracker, 12);
    tracker.setSource(2, 2, sourceTracker, 1);
    tracker.setSource(4, 2, sourceTracker, 24);

    TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
    // middle, full context
    assertEquals("cdefghijklmnopqrstuvwx", response.getParts().get(0).getSourceContext());
    // context near the beginning
    assertEquals("abcdefghijklm", response.getParts().get(1).getSourceContext());
    // context near the end
    assertEquals("opqrstuvwxyz", response.getParts().get(2).getSourceContext());
  }

  @Test public void getContentTracker() {
    CharOriginTracker tracker = new CharOriginTracker();
    TrackerRepository.setInterestTracker(new Object(), tracker);

    tracker.append(new char[] {'a', 'b', 'c', 'd'}, 1, 2);

    TrackerDetailResponse response = trackerResource.get(tracker.getTrackerId());
    assertEquals(1, response.getParts().size());

    assertEquals("bc", response.getParts().get(0).getContent());
    assertNull(response.getParts().get(0).getSource());
  }
}
