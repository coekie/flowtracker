package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.tracker.TrackPart;
import be.coekaerts.wouter.flowtracker.tracker.Tracker;
import be.coekaerts.wouter.flowtracker.tracker.TrackerRepository;

public class StringHook {
	/**
	 * Injected into String.concat. Registers the results origin.
	 */
	public static String afterConcat(String result, String target, String other) {
		Tracker.setSource(result, 0, target.length(), target, 0);
		Tracker.setSource(result, target.length(), other.length(), other, 0);
		
		return result;
	}
	
	/**
	 * Injected into String.substring. Registers the results origin.
	 */
	public static void afterSubstring(String result, String target, int beginIndex, int endIndex) {
		if (result != target) { // if it's not the whole String
			Tracker.setSource(result, 0, endIndex - beginIndex, target, beginIndex);
		}
	}
	
	public static TrackPart getStringTrack(String str) {
		StringContentExtractor extractor = new StringContentExtractor();
		// contentEquals has been instrumented to deal with this extractor
		str.contentEquals(extractor); 
		
		Tracker valueTracker = TrackerRepository.getTracker(extractor.value);
		if (valueTracker == null) {
			return null;
		} else {
			return new TrackPart(valueTracker, extractor.offset, str.length());
		}
	}
	
	public static class StringContentExtractor implements CharSequence {
		private char[] value;
		private int offset;
		
		private StringContentExtractor() {
		}
		
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
