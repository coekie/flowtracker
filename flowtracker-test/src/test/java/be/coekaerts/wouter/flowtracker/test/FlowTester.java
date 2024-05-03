package be.coekaerts.wouter.flowtracker.test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerPoint;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper for testing instrumentation of FlowAnalyzingTransformer and its TrackableValues and
 * Stores.
 * <p>
 * This class gets some special instrumentation, see {@code TesterStore} and {@code TesterValue} and
 * their usage.
 */
class FlowTester {
  private final List<Tracker> sources = new ArrayList<>();

  /**
   * Returns the char value, and treats this FlowTester as the source.
   * Calls to this method should get replaced by {@link #$tracked_createSourceChar(char)}.
   */
  char createSourceChar(@SuppressWarnings("unused") char c) {
    throw sourceNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterValue instrumentation
  char $tracked_createSourceChar(char c) {
    sources.add(new FixedOriginTracker(-1));
    return c;
  }

  /**
   * Returns the byte value, and treats this FlowTester as the source.
   * Calls to this method should get replaced by {@link #$tracked_createSourceByte(byte)}.
   */
  byte createSourceByte(@SuppressWarnings("unused") byte b) {
    throw sourceNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterValue instrumentation
  byte $tracked_createSourceByte(byte b) {
    sources.add(new FixedOriginTracker(-1));
    return b;
  }

  /**
   * Returns the short value, and treats this FlowTester as the source.
   * Calls to this method should get replaced by {@link #$tracked_createSourceShort(short)}.
   */
  short createSourceShort(@SuppressWarnings("unused") short s) {
    throw sourceNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterValue instrumentation
  short $tracked_createSourceShort(short s) {
    sources.add(new FixedOriginTracker(-1));
    return s;
  }

  /**
   * Returns the int value, and treats this FlowTester as the source.
   * Calls to this method should get replaced by {@link #$tracked_createSourceByte(byte)}.
   */
  int createSourceInt(@SuppressWarnings("unused") int i) {
    throw sourceNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterValue instrumentation
  int $tracked_createSourceInt(int i) {
    sources.add(new FixedOriginTracker(-1));
    return i;
  }

  // TODO this might be useful, but unused for now...
//  <T> T createSource(T t) {
//    sources.add(TrackerRepository.createFixedOriginTracker(t, -1));
//    return t;
//  }

  Tracker theSource() {
    assertWithMessage("theSource should only be used if there is exactly one source")
        .that(sources).hasSize(1);
    return sources.get(0);
  }

  /** Index in {@link #theSource()} that we pretend our values in this tester come from */
  int theSourceIndex() {
    return 42;
  }

  TrackerPoint theSourcePoint() {
    return TrackerPoint.of(theSource(), theSourceIndex());
  }

  /**
   * Assert that the given value is tracked as coming from index {@code expectedIndex} in
   * {@code expectedSource}.
   * Calls to this method should get replaced by
   * {@link #$tracked_assertTrackedValue(char, char, Tracker, int, TrackerPoint)}.
   */
  static void assertTrackedValue(@SuppressWarnings("unused") char c,
      @SuppressWarnings("unused") char expected,
      @SuppressWarnings("unused") Tracker expectedSource,
      @SuppressWarnings("unused") int expectedIndex) {
    throw valueNotTrackedError();
  }

  /**
   * Assert that the given value is tracked as coming from index {@code expectedIndex} in
   * {@code expectedSource}.
   * Calls to this method should get replaced by
   * {@link #$tracked_assertTrackedValue(byte, byte, Tracker, int, TrackerPoint)}.
   */
  static void assertTrackedValue(@SuppressWarnings("unused") byte b,
      @SuppressWarnings("unused") byte expected,
      @SuppressWarnings("unused") Tracker expectedSource,
      @SuppressWarnings("unused") int expectedIndex) {
    throw valueNotTrackedError();
  }

  /**
   * Assert that the given value is tracked as coming from index {@code expectedIndex} in
   * {@code expectedSource}.
   * Calls to this method should get replaced by
   * {@link #$tracked_assertTrackedValue(int, int, Tracker, int, TrackerPoint)}.
   */
  static void assertTrackedValue(@SuppressWarnings("unused") int i,
      @SuppressWarnings("unused") int expected,
      @SuppressWarnings("unused") Tracker expectedSource,
      @SuppressWarnings("unused") int expectedIndex) {
    throw valueNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static void $tracked_assertTrackedValue(char c, char expected,
      Tracker expectedTracker, int expectedIndex,
      TrackerPoint actual) {
    assertThat(c).isEqualTo(expected);
    assertThat(actual).isNotNull();
    assertThat(actual.tracker).isEqualTo(expectedTracker);
    assertThat(actual.index).isEqualTo(expectedIndex);
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static void $tracked_assertTrackedValue(byte b, byte expected,
      Tracker expectedTracker, int expectedIndex,
      TrackerPoint actual) {
    assertThat(b).isEqualTo(expected);
    assertThat(actual).isNotNull();
    assertThat(actual.tracker).isEqualTo(expectedTracker);
    assertThat(actual.index).isEqualTo(expectedIndex);
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static void $tracked_assertTrackedValue(int i, int expected,
      Tracker expectedTracker, int expectedIndex,
      TrackerPoint actual) {
    assertThat(i).isEqualTo(expected);
    assertThat(actual).isNotNull();
    assertThat(actual.tracker).isEqualTo(expectedTracker);
    assertThat(actual.index).isEqualTo(expectedIndex);
  }

  /**
   * Assert that the given value is tracked as coming from this tester.
   * Calls to this method should get replaced by
   * {@link #$tracked_assertIsTheTrackedValue(char, TrackerPoint)}.
   */
  void assertIsTheTrackedValue(@SuppressWarnings("unused") char c) {
    throw valueNotTrackedError();
  }

  /**
   * Assert that the given value is tracked as coming from this tester.
   * Calls to this method should get replaced by
   * {@link #$tracked_assertIsTheTrackedValue(byte, TrackerPoint)}.
   */
  void assertIsTheTrackedValue(@SuppressWarnings("unused") byte b) {
    throw valueNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  void $tracked_assertIsTheTrackedValue(char c, TrackerPoint actual) {
    assertThat(actual).isEqualTo(theSourcePoint());
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  void $tracked_assertIsTheTrackedValue(byte b, TrackerPoint actual) {
    assertThat(actual).isEqualTo(theSourcePoint());
  }

  /**
   * Returns the TrackerPoint that the value is tracked as coming from
   */
  static TrackerPoint getCharSourcePoint(@SuppressWarnings("unused") char c) {
    throw valueNotTrackedError();
  }

  /**
   * Returns the TrackerPoint that the value is tracked as coming from
   */
  static TrackerPoint getByteSourcePoint(@SuppressWarnings("unused") byte b) {
    throw valueNotTrackedError();
  }

  /**
   * Returns the TrackerPoint that the value is tracked as coming from
   */
  static TrackerPoint getIntSourcePoint(@SuppressWarnings("unused") int i) {
    throw valueNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static TrackerPoint $tracked_getCharSourcePoint(char c, TrackerPoint point) {
    return point;
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static TrackerPoint $tracked_getByteSourcePoint(byte b, TrackerPoint point) {
    return point;
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static TrackerPoint $tracked_getIntSourcePoint(int i, TrackerPoint point) {
    return point;
  }

  static char untrackedChar(char c) {
    return c;
  }

  private static AssertionError sourceNotTrackedError() {
    // This probably means that TesterValue.ensureTracked/insertTrackStatements was not called for
    // this value, because there was no instrumentation for where the value got stored.
    // methods in this class that call this were expected to get replaced by their "$tracked_"
    // equivalent by the instrumentation.
    return new AssertionError("source value was expected to get tracked, but wasn't.");
  }

  private static AssertionError valueNotTrackedError() {
    // This probably means that TesterStore.insertTrackStatements did not find a TrackableValue,
    // because there was no instrumentation for where the value came from.
    // methods in this class that call this were expected to get replaced by their "$tracked_"
    // equivalent by the instrumentation.
    return new AssertionError("given value was expected to get tracked, but wasn't.");
  }
}
