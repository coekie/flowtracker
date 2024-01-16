package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.assertEquals;

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
  static {
    // make sure the necessary classes are resolved in the current classloader, to avoid class
    // loading triggering at the wrong time. (when we're in the middle of handling an "Invocation";
    // class loading can screw up Invocation.pending).
    // this is probably a bug we need to fix another way. should we pre-resolve these classes in the
    // app classloader? (and hook creation of other classloaders and do the same there?)
    new FixedOriginTracker(-1);
    TrackerPoint.ofNullable(null, -1);
  }

  private final List<Tracker> sources = new ArrayList<>();

  /**
   * Returns the char value, and treats this char as the source.
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
   * Returns the byte value, and treats this byte as the source.
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

  // TODO this might be useful, but unused for now...
//  <T> T createSource(T t) {
//    sources.add(TrackerRepository.createFixedOriginTracker(t, -1));
//    return t;
//  }

  Tracker theSource() {
    assertEquals("theSource should only be used if there is exactly one source",
        1, sources.size());
    return sources.get(0);
  }

  /** Index in {@link #theSource()} that we pretend our values in this tester come from */
  int theSourceIndex() {
    return 42;
  }

  @SuppressWarnings("unused") // invoked by instrumentation
  TrackerPoint theSourcePoint() {
    return TrackerPoint.of(theSource(), theSourceIndex());
  }

  /**
   * Assert that the given value is tracked as coming from index {@code expectedIndex} in
   * {@code expectedSource}.
   * Calls to this method should get replaced by
   * {@link #$tracked_assertTrackedValue(char, char, Tracker, int, Tracker, int)}.
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
   * {@link #$tracked_assertTrackedValue(byte, byte, Tracker, int, Tracker, int)}.
   */
  static void assertTrackedValue(@SuppressWarnings("unused") byte b,
      @SuppressWarnings("unused") byte expected,
      @SuppressWarnings("unused") Tracker expectedSource,
      @SuppressWarnings("unused") int expectedIndex) {
    throw valueNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static void $tracked_assertTrackedValue(char c, char expected,
      Tracker expectedTracker, int expectedIndex,
      Tracker actualTracker, int actualIndex) {
    assertEquals(expected, c);
    assertEquals(expectedTracker, actualTracker);
    assertEquals(expectedIndex, actualIndex);
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static void $tracked_assertTrackedValue(byte b, byte expected,
      Tracker expectedTracker, int expectedIndex,
      Tracker actualTracker, int actualIndex) {
    assertEquals(expected, b);
    assertEquals(expectedTracker, actualTracker);
    assertEquals(expectedIndex, actualIndex);
  }

  /**
   * Assert that the given value is tracked as coming from this tester.
   * Calls to this method should get replaced by
   * {@link #$tracked_assertIsTheTrackedValue(char, Tracker, int)}.
   */
  void assertIsTheTrackedValue(@SuppressWarnings("unused") char c) {
    throw valueNotTrackedError();
  }

  /**
   * Assert that the given value is tracked as coming from this tester.
   * Calls to this method should get replaced by
   * {@link #$tracked_assertIsTheTrackedValue(byte, Tracker, int)}.
   */
  void assertIsTheTrackedValue(@SuppressWarnings("unused") byte b) {
    throw valueNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  void $tracked_assertIsTheTrackedValue(char c, Tracker actualTracker, int actualIndex) {
    assertEquals(theSource(), actualTracker);
    assertEquals(theSourceIndex(), actualIndex);
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  void $tracked_assertIsTheTrackedValue(byte b, Tracker actualTracker, int actualIndex) {
    assertEquals(theSource(), actualTracker);
    assertEquals(theSourceIndex(), actualIndex);
  }

  /** Returns the Tracker from which the value is tracked as coming */
  static Tracker getCharSourceTracker(@SuppressWarnings("unused") char value) {
    throw valueNotTrackedError();
  }

  /** Returns the Tracker from which the value is tracked as coming */
  static Tracker getByteSourceTracker(@SuppressWarnings("unused") byte value) {
    throw valueNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static Tracker $tracked_getCharSourceTracker(char value, Tracker tracker, int index) {
    return tracker;
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static Tracker $tracked_getByteSourceTracker(byte value, Tracker tracker, int index) {
    return tracker;
  }

  /**
   * Returns the index in {@link #getCharSourceTracker(char)} that the value is tracked as coming
   * from
   */
  static int getCharSourceIndex(@SuppressWarnings("unused") char c) {
    throw valueNotTrackedError();
  }

  /**
   * Returns the index in {@link #getByteSourceTracker(byte)} that the value is tracked as coming
   * from
   */
  static int getByteSourceIndex(@SuppressWarnings("unused") byte b) {
    throw valueNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static int $tracked_getCharSourceIndex(char c, Tracker tracker, int index) {
    return index;
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static int $tracked_getByteSourceIndex(byte b, Tracker tracker, int index) {
    return index;
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
