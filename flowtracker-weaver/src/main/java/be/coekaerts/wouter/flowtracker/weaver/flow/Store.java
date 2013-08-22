package be.coekaerts.wouter.flowtracker.weaver.flow;

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
	private final Frame<BasicValue> frame;
	
	protected Store(AbstractInsnNode storeInsn, Frame<BasicValue> frame) {
//		this.storeInsn = storeInsn;
		this.frame = frame;
	}
	
	abstract void insertTrackStatements(MethodNode methodNode);
	
	protected BasicValue getStackFromTop(int indexFromTop) {
		return frame.getStack(frame.getStackSize() - indexFromTop - 1);
	}
}
