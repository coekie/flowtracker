package be.coekaerts.wouter.flowtracker.weaver;

import org.objectweb.asm.Type;

public class Types {
	public static final Type CHAR_ARRAY = Type.getType("[C");
	public static final Type BYTE_ARRAY = Type.getType("[B");
	public static final Type OBJECT = Type.getObjectType("java/lang/Object");
	public static final Type INVOCATION =
			Type.getObjectType("be/coekaerts/wouter/flowtracker/tracker/Invocation");
}
