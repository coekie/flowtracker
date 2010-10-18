package be.coekaerts.wouter.flowtracker.test;

import org.junit.Test;

public class CharArrayTest {
	@Test
	public void charAt() {
		String a = "abc";
		String b = "def";
		String c = a.concat(b);
		
		char[] array = new char[10];
		array[0] = c.charAt(1);
		// <- CharArrayHistory.put(array, 0, c, 1) ???
	}
	
	// This one is hard.
	// Both assignments into array come from the same statement,
	// but x does not contain the *last* execution of that statement anymore.
	// Optimally, we should follow the flow of these local variables.
	// Or we should at least detect this, and mark the origin as unknown.
	@Test
	public void charAtFlow() {
		String a = "abc";
		String b = "def";
		String c = a.concat(b);
		
		char[] array = new char[10];
		
		char x = 0;
		char y = 0;
		
		for (int i = 0; i < 2; i++) {
			x = y;
			y = c.charAt(1);
		}
		
		array[0] = x;
		array[1] = y;
	}
	
	// we store the origin of a value before we actually call the method,
	// so what happens if it throws an exception...
	@Test
	public void charAtException() {
		String a = "abc";
		
		char[] array = new char[10];
		
		char x = 0;
		
		try {
			x = a.charAt(1000);
		} catch (IndexOutOfBoundsException e) {
		}
		array[0] = x;
	}
}
