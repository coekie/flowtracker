package be.coekaerts.wouter.flowtracker.hook;

import be.coekaerts.wouter.flowtracker.history.ConcatOrigin;
import be.coekaerts.wouter.flowtracker.history.StringHistory;
import be.coekaerts.wouter.flowtracker.history.SubstringOrigin;

public class StringHook {
	public static String afterConcat(String result, String target, String other) {
		// keep track of concatenation of empty String
		// if it would return the same String instance, create a fresh one
		if (result == target) {
			result = new String(result);
		}
		
		StringHistory.createHistory(result, new ConcatOrigin(target, other));
		
		return result;
	}
	
	public static void afterSubstring(String result, String target, int beginIndex, int endIndex) {
		if (result != target) { // if it's not the whole String
			StringHistory.createHistory(result, new SubstringOrigin(target, beginIndex, endIndex));
		}
	}
}
