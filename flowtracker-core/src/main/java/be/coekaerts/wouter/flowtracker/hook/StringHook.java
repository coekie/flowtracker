package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;
import be.coekaerts.wouter.flowtracker.tracker.Trackers;

public class StringHook {
  public static final String DEBUG_UNTRACKED = "debugUntracked";

  private static String debugUntracked = null;

  public static void initDebugUntracked(String debugUntrackedConfig) {
    debugUntracked = debugUntrackedConfig;
  }

  public static Tracker getStringTracker(String str) {
    StringContentExtractor extractor = new StringContentExtractor(str);
    return TrackerRepository.getTracker(extractor.value);
  }

  @SuppressWarnings("unused") // used by instrumented code
  public static Tracker getCharSequenceTracker(CharSequence cs) {
    // for now we only support Strings (when we support more this should move somewhere else)
    return cs instanceof String ? getStringTracker((String) cs) : null;
  }

  public static void createFixedOriginTracker(String str) {
    StringContentExtractor extractor = new StringContentExtractor(str);

    TrackerRepository.createFixedOriginTracker(extractor.value, str.length());
  }

  @SuppressWarnings("UnusedDeclaration") // used by instrumented code
  public static void afterInit(String target) {
    if (debugUntracked != null && target.contains(debugUntracked)
        && getStringTracker(target) == null
        // ignore the specifying of the debugUntracked string on the command line itself
        // (but eventually that should be tracked too, see java.lang.ProcessingEnvironment)
        && !target.contains("debugUntracked")) {
      Trackers.suspendOnCurrentThread();
      new Throwable("untracked").printStackTrace();
      Trackers.unsuspendOnCurrentThread();
    }
  }

  /** Get a tracker even when trackers are suspected; to be used from a debugger. */
  @SuppressWarnings("unused")
  public static Tracker forceGetStringTracker(String str) {
    if (Trackers.isActive()) return getStringTracker(str);
    Trackers.unsuspendOnCurrentThread();
    Tracker result = getStringTracker(str);
    Trackers.suspendOnCurrentThread();
    return result;
  }

  /**
   * Used by {@link StringHook#getStringTracker(String)} to retrieve String.value and String.offset.
   * <p>
   * The only reason this class implements {@link CharSequence} is so that it can be passed to the
   * instrumented {@link String#contentEquals(CharSequence)} method.
   */
  public static class StringContentExtractor implements CharSequence {
    private char[] value;
    private int offset;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private StringContentExtractor(String str) {
      // contentEquals has been instrumented to deal with this extractor.
      str.contentEquals(this);
    }

    /**
     * Called by {@link String#contentEquals(CharSequence)}, to expose the String content.
     */
    @SuppressWarnings("UnusedDeclaration")
    public void setContent(char[] value, int offset) {
      this.value = value;
      this.offset = offset;
    }

    @Override
    public int length() {
      throw new UnsupportedOperationException("This is not supposed to get called. "
          + "Hint: was String.contentEquals was not instrumented because the agent is not loaded?");
    }

    @Override
    public char charAt(int index) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      throw new UnsupportedOperationException();
    }
  }
}
