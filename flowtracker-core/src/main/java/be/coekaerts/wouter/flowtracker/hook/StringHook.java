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

  public static void createFixedOriginTracker(String str) {
    StringContentExtractor extractor = new StringContentExtractor(str);

    TrackerRepository.createFixedOriginTracker(extractor.value, str.length());
  }

  @SuppressWarnings("UnusedDeclaration") // used by instrumented code
  public static void afterInit(String target) {
    if (debugUntracked != null && target.contains(debugUntracked)) {
      Trackers.suspendOnCurrentThread();
      new Throwable("untracked").printStackTrace();
      Trackers.unsuspendOnCurrentThread();
    }
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
