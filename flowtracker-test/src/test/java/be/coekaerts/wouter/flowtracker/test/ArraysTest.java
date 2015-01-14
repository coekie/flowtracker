package be.coekaerts.wouter.flowtracker.test;

import java.util.Arrays;
import org.junit.Test;

import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.assertPartsCompleteEqual;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.part;
import static be.coekaerts.wouter.flowtracker.test.TrackTestHelper.track;

/** Test for {@link Arrays}. */
public class ArraysTest {
  @Test public void copyOf() {
    char[] abcdef = track("abcdef".toCharArray());
    char[] abcd = Arrays.copyOf(abcdef, 4);
    assertPartsCompleteEqual(abcd, part(abcdef, 0, 4));
  }

  @Test public void copyOfRange() {
    char[] abcdef = track("abcdef".toCharArray());
    char[] bcde = Arrays.copyOfRange(abcdef, 1, 5);
    assertPartsCompleteEqual(bcde, part(abcdef, 1, 4));
  }
}
