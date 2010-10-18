package be.coekaerts.wouter.flowtracker.hook;


public class CharArrayHook {
	public static void setCharWithOrigin(char[] array, int arrayIndex, char value, Object source, int sourceIndex) {
		array[arrayIndex] = value;
//		System.out.println("CharArrayHook.setCharOrigin");
	}
}
