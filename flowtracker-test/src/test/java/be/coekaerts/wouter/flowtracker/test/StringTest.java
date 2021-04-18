package be.coekaerts.wouter.flowtracker.test;

import static be.coekaerts.wouter.flowtracker.hook.StringHook.getStringTracker;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.track;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.trackCopy;
import static be.coekaerts.wouter.flowtracker.tracker.TrackerSnapshot.snapshotBuilder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import org.junit.Test;

public class StringTest {
  @Test public void testUnknown() {
    String a = "unknownTest";
    assertNull(TrackerRepository.getTracker(a));
    assertNull(getStringTracker(a));
  }

  @Test public void testToCharArray() {
    String foo = trackCopy("foo");
    char[] array = foo.toCharArray();
    snapshotBuilder().trackString(foo).assertEquals(TrackerRepository.getTracker(array));
  }

  @Test public void testConcat() {
    String foo = trackCopy("foo");
    String bar = trackCopy("bar");
    String foobar = foo.concat(bar);
    assertEquals("foobar", foobar);

    snapshotBuilder().trackString(foo).trackString(bar)
        .assertEquals(getStringTracker(foobar));
  }

  @Test public void testSubstringBegin() {
    String foobar = trackCopy("foobar");
    String foo = foobar.substring(0, 3);
    assertEquals("foo", foo);

    snapshotBuilder().trackString(foobar, 0, 3)
        .assertEquals(getStringTracker(foo));
  }

  @Test public void testSubstringEnd() {
    String foobar = trackCopy("foobar");
    String bar = foobar.substring(3);
    assertEquals("bar", bar);

    snapshotBuilder().trackString(foobar, 3, 3)
        .assertEquals(getStringTracker(bar));
  }

  @Test public void testGetStringTracker() {
    char[] chars = track(new char[] {'a', 'b', 'c', 'd'});
    String str = new String(chars, 1, 2); // create String "bc"
    assertEquals("bc", str);

    snapshotBuilder().track(chars, 1, 2)
        .assertEquals(getStringTracker(str));
  }
}
