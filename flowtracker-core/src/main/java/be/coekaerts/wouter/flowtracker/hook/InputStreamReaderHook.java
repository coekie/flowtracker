package be.coekaerts.wouter.flowtracker.hook;

import java.io.InputStreamReader;

import be.coekaerts.wouter.flowtracker.history.ReaderHistory;

public class InputStreamReaderHook {
	
	public static void afterRead1(int result, InputStreamReader target) {
		if (result > 0) {
			ReaderHistory history = ReaderHistory.getHistory(target);
			if (history != null) {
				history.addRead((char) result);
			}
		}
	}
	
	public static void afterReadCharArray(int read, InputStreamReader target, char[] cbuf) {
		afterReadCharArrayOffset(read, target, cbuf, 0);
	}
	
	public static void afterReadCharArrayOffset(int read, InputStreamReader target, char[] cbuf, int offset) {
		if (read >= 0) {
			ReaderHistory history = ReaderHistory.getHistory(target);
			if (history != null) {
				history.addRead(cbuf, offset, read);
			}
		}
	}
	
	// SHORTCUT: int read(CharBuffer):
	//  assume super implementation will delegate to another read method that we already handle
	// alternatives:
	// * delegate to another read method ourselves
	// * get the result back using target.subsequence()
}
