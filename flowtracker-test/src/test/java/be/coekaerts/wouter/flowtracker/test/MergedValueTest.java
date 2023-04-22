package be.coekaerts.wouter.flowtracker.test;

import org.junit.Ignore;
import org.junit.Test;

public class MergedValueTest {
  @SuppressWarnings("FieldCanBeLocal")
  private final boolean yes = true;
  @SuppressWarnings("FieldCanBeLocal")
  private final boolean no = false;

  @Test
  public void testTernaryOperatorCreatingValueYes() {
    FlowTester tester1 = new FlowTester();
    FlowTester tester2 = new FlowTester();

    tester1.assertIsTheTrackedValue(
        yes ? tester1.createSourceChar('a') : tester2.createSourceChar('b'));
  }

  @Test
  public void testTernaryOperatorCreatingValueNo() {
    FlowTester tester1 = new FlowTester();
    FlowTester tester2 = new FlowTester();

    tester2.assertIsTheTrackedValue(
        no ? tester1.createSourceChar('a') : tester2.createSourceChar('b'));
  }

  @Test
  @Ignore // TODO MergedValue copying
  public void testTernaryOperatorCopyingValueYes() {
    FlowTester tester1 = new FlowTester();
    FlowTester tester2 = new FlowTester();
    char a = tester1.createSourceChar('a');
    char b = tester2.createSourceChar('b');

    tester1.assertIsTheTrackedValue(yes ? a : b);
  }

  @Test
  @Ignore // TODO MergedValue copying
  public void testTernaryOperatorCopyingValueNo() {
    FlowTester tester1 = new FlowTester();
    FlowTester tester2 = new FlowTester();
    char a = tester1.createSourceChar('a');
    char b = tester2.createSourceChar('b');

    tester2.assertIsTheTrackedValue(no ? a : b);
  }

  @Test
  public void testIfCreatingValueYes() {
    FlowTester tester1 = new FlowTester();
    FlowTester tester2 = new FlowTester();
    char r;
    if (yes) {
      r = tester1.createSourceChar('a');
    } else {
      r = tester2.createSourceChar('b');
    }

    tester1.assertIsTheTrackedValue(r);
  }

  @Test
  public void testIfCreatingValueNo() {
    FlowTester tester1 = new FlowTester();
    FlowTester tester2 = new FlowTester();
    char r;
    if (no) {
      r = tester1.createSourceChar('a');
    } else {
      r = tester2.createSourceChar('b');
    }

    tester2.assertIsTheTrackedValue(r);
  }

  @Test
  @Ignore // TODO MergedValue copying
  public void testIfCopyingValueYes() {
    FlowTester tester1 = new FlowTester();
    FlowTester tester2 = new FlowTester();
    char a = tester1.createSourceChar('a');
    char b = tester2.createSourceChar('b');
    char r;
    if (yes) {
      r = a;
    } else {
      r = b;
    }

    tester1.assertIsTheTrackedValue(r);
  }

  @Test
  @Ignore // TODO MergedValue copying
  public void testIfCopyingValueNo() {
    FlowTester tester1 = new FlowTester();
    FlowTester tester2 = new FlowTester();
    char a = tester1.createSourceChar('a');
    char b = tester2.createSourceChar('b');
    char r;
    if (no) {
      r = a;
    } else {
      r = b;
    }

    tester2.assertIsTheTrackedValue(r);
  }
}
