package be.coekaerts.wouter.flowtracker.weaver;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Value;

public class CharArrayClassTransformer implements ClassAdapterFactory {
	private static class CharArrayClassAdapter extends ClassAdapter {
		private String name;

		public CharArrayClassAdapter(ClassVisitor cv) {
			super(cv);
		}
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			this.name = name;
		}
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			CharArrayMethodAdapter methodAdapter = new CharArrayMethodAdapter(mv, this.name, access, name, desc, signature, exceptions);
			return methodAdapter;
		}
	}
	
	private static class CharArrayMethodAdapter extends MethodNode {
		private final String owner;
		private final MethodVisitor mv;
		
		public CharArrayMethodAdapter(MethodVisitor mv, String owner, int access, String name, String desc, String signature, String[] exceptions) {
			super(access, name, desc, signature, exceptions);
			this.owner = owner;
			this.mv = mv;
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			CharArrayInterpreter interpreter = new CharArrayInterpreter();
			Analyzer analyzer = new Analyzer(interpreter);
			try {
				analyzer.analyze(owner, this);
			} catch (AnalyzerException e) {
				throw new RuntimeException(e);
			}
			
//			Frame[] frames = analyzer.getFrames();
			
			for (Map.Entry<MethodInsnNode, ExtractedCharValue> entry : interpreter.trackedReturnValues.entrySet()) {
				MethodInsnNode mInsn = entry.getKey();
				ExtractedCharValue value = entry.getValue();
				
				value.setOriginLocals(this.maxLocals);
				
				// on the stack:  String target, int index
				int targetStringLocal = this.maxLocals++;
				int indexLocal = this.maxLocals++;
				
				InsnList toInsert = new InsnList();
				
				// we could avoid some local-to-stack copying by dupping
//				toInsert.add(new InsnNode(Opcodes.DUP2));
				
				toInsert.add(new VarInsnNode(Opcodes.ISTORE, indexLocal));
				toInsert.add(new VarInsnNode(Opcodes.ASTORE, targetStringLocal));
				
				toInsert.add(new VarInsnNode(Opcodes.ALOAD, targetStringLocal));
				toInsert.add(new VarInsnNode(Opcodes.ILOAD, indexLocal));
				
				instructions.insertBefore(mInsn, toInsert);
			}
			
			for (Map.Entry<InsnNode, ExtractedCharValue> entry : interpreter.trackedCharArrayStores.entrySet()) {
				InsnNode storeInsn = entry.getKey();
				ExtractedCharValue value = entry.getValue();
				
				// on the stack: char[] target, int index, char toStore
				
				InsnList toInsert = new InsnList();
				
				if (value != null) {
					int targetStringLocal = value.getOriginLocals();
					int indexLocal = targetStringLocal + 1;
					toInsert.add(new VarInsnNode(Opcodes.ALOAD, targetStringLocal));
					toInsert.add(new VarInsnNode(Opcodes.ILOAD, indexLocal));
				} else {
					toInsert.add(new InsnNode(Opcodes.ACONST_NULL));
					toInsert.add(new InsnNode(Opcodes.ICONST_0));
				}
				maxStack += 2;
				
				Method hook = Method.getMethod("void setCharWithOrigin(char[],int,char,Object,int)");
				
				toInsert.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "be/coekaerts/wouter/flowtracker/hook/CharArrayHook", hook.getName(), hook.getDescriptor()));
				
				instructions.insert(storeInsn, toInsert);
				instructions.remove(storeInsn); // our hook takes care of the storing
			}
			
			
			this.accept(mv); // send the result to the next MethodVisitor
			
		}
	}
	
	private static class CharArrayInterpreter extends BasicInterpreter {
		/**
		 * For every method call for which we want to keep track of its return value,
		 * maps that method call to its return value.
		 */
		private final Map<MethodInsnNode, ExtractedCharValue> trackedReturnValues = new IdentityHashMap<MethodInsnNode, ExtractedCharValue>();
		
		/**
		 * For every array store in a char[], maps that store instruction to its origin ExtractedCharValue,
		 * or to null if the origin is unknown.
		 */
		private final Map<InsnNode, ExtractedCharValue> trackedCharArrayStores = new IdentityHashMap<InsnNode, ExtractedCharValue>();
		
		@Override
		public Value newValue(Type type) {
			// for char[], remember the exact type
	        if (CHAR_ARRAY_TYPE.equals(type)) {
				return new BasicValue(type);
			}
	        // for others the exact type doesn't matter
			return super.newValue(type);
		}
		
		@Override
		public Value naryOperation(AbstractInsnNode insn, List values) throws AnalyzerException {
			if (insn instanceof MethodInsnNode) {
				MethodInsnNode mInsn = (MethodInsnNode) insn;
				if ("java/lang/String".equals(mInsn.owner) && "charAt".equals(mInsn.name) && "(I)C".equals(mInsn.desc)) {
					return new ExtractedCharValue(mInsn);
				}
			}
			return super.naryOperation(insn, values);
		}
		
		@Override
		public Value ternaryOperation(AbstractInsnNode insn, Value value1, Value value2, Value value3)
				throws AnalyzerException {
			if (insn.getOpcode() == CASTORE) {
				InsnNode storeInsn = (InsnNode) insn;
				BasicValue charArrayValue = (BasicValue)value1;
				
				if (! CHAR_ARRAY_TYPE.equals(charArrayValue.getType())) {
					throw new AnalyzerException(insn, "CASTORE but not in a char array");
				}
				
				ExtractedCharValue charValue;
				
				if (value3 instanceof ExtractedCharValue) { // if we know where the value we are storing came from
					charValue = (ExtractedCharValue) value3;
					trackedReturnValues.put(charValue.getmInsn(), charValue);
				} else {
					charValue = null; // unknown
				}
				
				trackedCharArrayStores.put(storeInsn, charValue);
			}
			return super.ternaryOperation(insn, value1, value2, value3);
		}
	}
	
	/**
	 * A char of which we know where it came from
	 */
	private static class CharValue extends BasicValue {
		private CharValue() {
			super(Type.CHAR_TYPE);
		}
	}
	
	/**
	 * A char received from a method call (on an object that might be tracked...) 
	 */
	private static class ExtractedCharValue extends CharValue {
		private final MethodInsnNode mInsn;
		
		/**
		 * Index of the first local used to store the origin of this value.
		 */
		private int originLocals;
		
		private ExtractedCharValue(MethodInsnNode mInsn) {
			super();
			this.mInsn = mInsn;
			// Note: when it can return something else than char, we get type with Type.getReturnType(mInsn.desc)
		}
		
		MethodInsnNode getmInsn() {
			return mInsn;
		}
		
		void setOriginLocals(int originLocals) {
			this.originLocals = originLocals;
		}
		
		public int getOriginLocals() {
			return originLocals;
		}
	}
	
	
	
	private static final Type CHAR_ARRAY_TYPE = Type.getType("[C"); 
	
	public ClassVisitor createClassAdapter(ClassVisitor cv) {
		return new CharArrayClassAdapter(cv);
	}
}
