package com.coekie.flowtracker.weaver;

import org.objectweb.asm.Type;

public class Types {
	public static final Type CHAR_ARRAY = Type.getType("[C");
	public static final Type BYTE_ARRAY = Type.getType("[B");
	public static final Type OBJECT = Type.getObjectType("java/lang/Object");
	public static final Type STRING = Type.getObjectType("java/lang/String");
	public static final Type INVOCATION =
			Type.getObjectType("com/coekie/flowtracker/tracker/Invocation");
}
