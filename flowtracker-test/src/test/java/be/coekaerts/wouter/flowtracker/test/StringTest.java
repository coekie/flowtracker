package be.coekaerts.wouter.flowtracker.test;

import be.coekaerts.wouter.flowtracker.hook.StringHook;
import be.coekaerts.wouter.flowtracker.tracker.PartTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import org.junit.Test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.assertStringOriginPartsCompleteEqual;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.assertTrackerPartsCompleteEqual;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.part;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.strPart;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.track;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StringTest {
  @Test public void unkownTest() {
    String a = "unkownTest";
    assertNull(TrackerRepository.getTracker(a));
    assertNull(StringHook.getStringTrack(a));
  }

  @Test public void concatTest() {
    String a = trackCopy("concatTest1");
    String b = trackCopy("concatTest2");
    String ab = a.concat(b);
    assertEquals("concatTest1concatTest2", ab);

    assertStringOriginPartsCompleteEqual(ab, strPart(a), strPart(b));
  }

  @Test public void substringBeginTest() {
    String foobar = trackCopy("foobar");
    String foo = foobar.substring(0, 3);
    assertEquals("foo", foo);

    assertStringOriginPartsCompleteEqual(foo, strPart(foobar, 0, 3));
  }

  @Test public void substringEndTest() {
    String foobar = trackCopy("foobar");
    String bar = foobar.substring(3);
    assertEquals("bar", bar);

    assertStringOriginPartsCompleteEqual(bar, strPart(foobar, 3, 3));
  }

  @Test public void stringTrackTest() {
    char[] chars = track(new char[] {'a', 'b', 'c', 'd'});
    String str = new String(chars, 1, 2); // create String "bc"
    assertEquals("bc", str);

    // stringTrack points to the String.value char[]
    PartTracker stringTrack = StringHook.getStringTrack(str);
    assertNotNull(stringTrack);
    assertEquals(0, stringTrack.getIndex());
    assertEquals(2, stringTrack.getLength());

    // String.value is copy of part of our chars
    Tracker stringValueTracker = stringTrack.getTracker();
    assertNotNull(stringValueTracker);
    assertTrackerPartsCompleteEqual(stringValueTracker, part(chars, 1, 2));
  }

  /** Test that we didn't break the normal functioning of contentEquals. */
  @Test public void contentEqualsTest() {
    String str = "abcd";
    StringBuilder sb = new StringBuilder();
    sb.append("ab").append("cd");
    assertTrue(str.contentEquals(sb));
    assertFalse(str.contentEquals("foo"));
  }
}
