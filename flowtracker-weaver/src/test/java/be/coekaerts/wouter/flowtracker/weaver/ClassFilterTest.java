package be.coekaerts.wouter.flowtracker.weaver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ClassFilterTest {
  @Test
  public void test() {
    ClassFilter filter = new ClassFilter("+foobar,-foo*,+fo*", "");
    assertTrue(filter.include("foobar")); // exact match
    assertFalse(filter.include("foobarbaz")); // exact match is not a prefix
    assertFalse(filter.include("foo")); // excluded by second rule
    assertFalse(filter.include("foox")); // excluded by second rule (prefix)
    assertTrue(filter.include("fox")); // included by third rule (prefix)
    assertFalse(filter.include("bar")); // default value
  }

  @Test
  public void testSlashVsDot() {
    ClassFilter filter = new ClassFilter("+foo.bar.A,+foo.bar.B*", "");
    assertTrue(filter.include("foo/bar/A"));
    assertTrue(filter.include("foo/bar/Bee"));
    assertFalse(filter.include("foo/bar/C"));
  }

  @Test
  public void testInnerClass() {
    ClassFilter filter = new ClassFilter("+foo.Bar", "");
    assertTrue(filter.include("foo/Bar"));
    assertTrue(filter.include("foo/Bar$Inner"));
    assertFalse(filter.include("foo/Barz"));
  }

  @Test
  public void testRecommended() {
    String recommended = "+foo*,-bar*";
    ClassFilter filter = new ClassFilter("-foo.override,+bar.override,%recommended", recommended);
    assertTrue(filter.include("foo/A"));
    assertFalse(filter.include("bar/B"));
    assertFalse(filter.include("foo/override"));
    assertTrue(filter.include("bar/override"));
  }

}