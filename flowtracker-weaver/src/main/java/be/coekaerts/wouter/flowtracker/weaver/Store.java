package be.coekaerts.wouter.flowtracker.weaver;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

/**
 * Represents an instruction that stores a (possibly tracked) value
 * in an object.
 */
abstract class Store {
//	private final AbstractInsnNode storeInsn;
	private final Frame frame;
	
	protected Store(AbstractInsnNode storeInsn, Frame frame) {
//		this.storeInsn = storeInsn;
		this.frame = frame;
	}
	
	abstract void insertTrackStatements(MethodNode methodNode);
	
	protected Frame getFrame() {
		return frame;
	}
	
	protected BasicValue getStackFromTop(int indexFromTop) {
		return (BasicValue) frame.getStack(frame.getStackSize() - indexFromTop - 1);
	}
}
