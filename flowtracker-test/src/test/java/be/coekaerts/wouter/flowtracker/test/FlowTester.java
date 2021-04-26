package be.coekaerts.wouter.flowtracker.test;

import static org.junit.Assert.assertEquals;

import be.coekaerts.wouter.flowtracker.tracker.FixedOriginTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper for testing instrumentation of FlowAnalyzingTransformer and its TrackableValues and Stores
 */
class FlowTester {
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

  // TODO this might be useful, but unused for now...
  <T> T createSource(T t) {
    sources.add(TrackerRepository.createFixedOriginTracker(t, -1));
    return t;
  }

  Tracker theSource() {
    assertEquals("theSource should only be used if there is exactly one source",
        1, sources.size());
    return sources.get(0);
  }

  /** Index in {@link #theSource()} that we pretend our values in this tester come from */
  int theSourceIndex() {
    return 42;
  }

  /**
   * Assert that the the given value is tracked as coming from index {@code expectedIndex} in
   * {@code expectedSource}.
   * Calls to this method should get replaced by
   * {@link #$tracked_assertTrackedValue(char, Tracker, int, Tracker, int)}.
   */
  static void assertTrackedValue(@SuppressWarnings("unused") char c,
      @SuppressWarnings("unused") Tracker expectedSource,
      @SuppressWarnings("unused") int expectedIndex) {
    throw valueNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static void $tracked_assertTrackedValue(char c, Tracker expectedTracker, int expectedIndex,
      Tracker actualTracker, int actualIndex) {
    assertEquals(expectedTracker, actualTracker);
    assertEquals(expectedIndex, actualIndex);
  }

  /**
   * Assert that the the given value is tracked as coming from this tester.
   * Calls to this method should get replaced by
   * {@link #$tracked_assertIsTheTrackedValue(char, Tracker, int)}.
   */
  void assertIsTheTrackedValue(@SuppressWarnings("unused") char c) {
    throw valueNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  void $tracked_assertIsTheTrackedValue(char c, Tracker actualTracker, int actualIndex) {
    assertEquals(theSource(), actualTracker);
    assertEquals(theSourceIndex(), actualIndex);
  }

  /** Returns the Tracker from which the value is tracked as coming */
  static Tracker getCharSourceTracker(@SuppressWarnings("unused") char value) {
    throw valueNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static Tracker $tracked_getCharSourceTracker(char value, Tracker tracker, int index) {
    return tracker;
  }

  /**
   * Returns the index in {@link #getCharSourceTracker(char)} that the value is tracked as coming
   * from
   */
  static int getCharSourceIndex(@SuppressWarnings("unused") char c) {
    throw valueNotTrackedError();
  }

  @SuppressWarnings("unused") // invoked by TesterStore instrumentation
  static int $tracked_getCharSourceIndex(char c, Tracker tracker, int index) {
    return index;
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
