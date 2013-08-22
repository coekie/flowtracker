package be.coekaerts.wouter.flowtracker.weaver.flow;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import be.coekaerts.wouter.flowtracker.weaver.Types;

/**
 * The storing of a char in a char[].
 */
// on the stack: char[] target, int index, char toStore 
class CaStore extends Store {
	private final InsnNode storeInsn;
	
	CaStore(InsnNode storeInsn, Frame<BasicValue> frame) {
		super(storeInsn, frame);
		this.storeInsn = storeInsn;
		
		assert Types.CHAR_ARRAY.equals(getStackFromTop(2).getType());
	}
	
	void insertTrackStatements(MethodNode methodNode) {
		BasicValue stored = getStored();
		
		InsnList toInsert = new InsnList();
		
		if (stored instanceof TrackableValue) { // if we know where the value we are storing came from
			TrackableValue trackedStored = (TrackableValue) stored;
			trackedStored.ensureTracked(methodNode);
			
			trackedStored.loadSourceTracker(toInsert);
			trackedStored.loadSourceIndex(toInsert);
		} else {
			toInsert.add(new InsnNode(Opcodes.ACONST_NULL));
			toInsert.add(new InsnNode(Opcodes.ICONST_0));
		}
		
		methodNode.maxStack += 2; // TODO using the frame info we could optimize this
		
		Method hook = Method.getMethod("void setCharWithOrigin(char[],int,char,be.coekaerts.wouter.flowtracker.tracker.Tracker,int)");
		
		toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "be/coekaerts/wouter/flowtracker/hook/CharArrayHook", hook.getName(), hook.getDescriptor()));
		
		methodNode.instructions.insert(storeInsn, toInsert);
		methodNode.instructions.remove(storeInsn); // our hook takes care of the storing
	}
	
	/**
	 * The value being stored
	 */
	private BasicValue getStored() {
		return getStackFromTop(0);
	}
}
