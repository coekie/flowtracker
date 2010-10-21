package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.history.Tracker;

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
}
