package com.coekie.flowtracker.weaver;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class ClassFilterTest {
  @Test
  public void test() {
    ClassFilter filter = new ClassFilter("+foobar,-foo*,+fo*", "");
    assertThat(filter.include("foobar")).isTrue(); // exact match
    assertThat(filter.include("foobarbaz")).isFalse(); // exact match is not a prefix
    assertThat(filter.include("foo")).isFalse(); // excluded by second rule
    assertThat(filter.include("foox")).isFalse(); // excluded by second rule (prefix)
    assertThat(filter.include("fox")).isTrue(); // included by third rule (prefix)
    assertThat(filter.include("bar")).isFalse(); // default value
  }

  @Test
  public void testSlashVsDot() {
    ClassFilter filter = new ClassFilter("+foo.bar.A,+foo.bar.B*", "");
    assertThat(filter.include("foo/bar/A")).isTrue();
    assertThat(filter.include("foo/bar/Bee")).isTrue();
    assertThat(filter.include("foo/bar/C")).isFalse();
  }

  @Test
  public void testInnerClass() {
    ClassFilter filter = new ClassFilter("+foo.Bar", "");
    assertThat(filter.include("foo/Bar")).isTrue();
    assertThat(filter.include("foo/Bar$Inner")).isTrue();
    assertThat(filter.include("foo/Barz")).isFalse();
  }

  @Test
  public void testRecommended() {
    String recommended = "+foo*,-bar*";
    ClassFilter filter = new ClassFilter("-foo.override,+bar.override,%base", recommended);
    assertThat(filter.include("foo/A")).isTrue();
    assertThat(filter.include("bar/B")).isFalse();
    assertThat(filter.include("foo/override")).isFalse();
    assertThat(filter.include("bar/override")).isTrue();
  }

}