package be.coekaerts.wouter.flowtracker.weaver;

import org.objectweb.asm.Type;

public class Types {
	public static final Type CHAR_ARRAY = Type.getType("[C");
	public static final Type STRING = Type.getType(String.class);
}
