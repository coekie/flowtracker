package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.PartTracker;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;

public class StringHook {
	
	public static PartTracker getStringTrack(String str) {
		StringContentExtractor extractor = new StringContentExtractor(str);
		
		Tracker valueTracker = TrackerRepository.getTracker(extractor.value);
		if (valueTracker == null) {
			return null;
		} else {
			return new PartTracker(valueTracker, extractor.offset, str.length());
		}
	}
	
	public static void createFixedOriginTracker(String str) {
		StringContentExtractor extractor = new StringContentExtractor(str);
		
		TrackerRepository.createFixedOriginTracker(extractor.value, str.length());
	}
	
	/**
	 * Used by {@link StringHook#getStringTrack(String)} to retrieve String.value and String.offset.
	 * <p>
	 * The only reason this class implements {@link CharSequence} is so that it can be passed to the
	 * instrumented {@link String#contentEquals(CharSequence)} method.
	 */
	public static class StringContentExtractor implements CharSequence {
		private char[] value;
		private int offset;
		
		private StringContentExtractor(String str) {
			// contentEquals has been instrumented to deal with this extractor.
			str.contentEquals(this);
		}
		
		/**
		 * Called by {@link String#contentEquals(CharSequence)}, to expose the String content.
		 */
		public void setContent(char[] value, int offset) {
			this.value = value;
			this.offset = offset;
		}
		
		@Override
		public int length() {
			throw new UnsupportedOperationException();
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
