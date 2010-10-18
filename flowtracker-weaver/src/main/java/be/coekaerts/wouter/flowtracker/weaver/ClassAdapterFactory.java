package be.coekaerts.wouter.flowtracker.weaver;

import org.objectweb.asm.ClassVisitor;

public interface ClassAdapterFactory {
	ClassVisitor createClassAdapter(ClassVisitor cv);
}
