package be.coekaerts.wouter.flowtracker.weaver;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
			
			for (TrackableValue value : interpreter.trackedValues) {
				value.insertTrackStatements(this);
			}
			
			for (Map.Entry<InsnNode, TrackableValue> entry : interpreter.trackedCharArrayStores.entrySet()) {
				InsnNode storeInsn = entry.getKey();
				TrackableValue value = entry.getValue();
				
				// on the stack: char[] target, int index, char toStore
				
				InsnList toInsert = new InsnList();
				
				if (value != null) {
					value.loadSourceObject(toInsert);
					value.loadSourceIndex(toInsert);
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
		 * {@link TrackableValue}s that we actually want to track.
		 */
		private final Set<TrackableValue> trackedValues = Collections.newSetFromMap(new IdentityHashMap<TrackableValue, Boolean>());
		
		/**
		 * For every array store in a char[], maps that store instruction to its origin CharAtValue,
		 * or to null if the origin is unknown.
		 */
		private final Map<InsnNode, TrackableValue> trackedCharArrayStores = new IdentityHashMap<InsnNode, TrackableValue>();
		
		@Override
		public Value newValue(Type type) {
			// for char[], remember the exact type
	        if (CHAR_ARRAY_TYPE.equals(type)) {
				return new BasicValue(type);
			}
	        // for others the exact type doesn't matter
			return super.newValue(type);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Value naryOperation(AbstractInsnNode insn, List values) throws AnalyzerException {
			if (insn instanceof MethodInsnNode) {
				MethodInsnNode mInsn = (MethodInsnNode) insn;
				if ("java/lang/String".equals(mInsn.owner) && "charAt".equals(mInsn.name) && "(I)C".equals(mInsn.desc)) {
					return new CharAtValue(mInsn);
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
				
				CharAtValue charValue;
				
				if (value3 instanceof CharAtValue) { // if we know where the value we are storing came from
					charValue = (CharAtValue) value3;
					trackedValues.add(charValue);
				} else {
					charValue = null; // unknown
				}
				
				trackedCharArrayStores.put(storeInsn, charValue);
			}
			return super.ternaryOperation(insn, value1, value2, value3);
		}
	}
	
	/**
	 * A value of which we can track where it came from
	 */
	private static abstract class TrackableValue extends BasicValue {
		private TrackableValue(Type type) {
			super(type);
		}
		
		/**
		 * Insert the statements needed to keep track of the origin of this value.
		 * 
		 * @param methodNode method to add the statements in, at the right place
		 */
		abstract void insertTrackStatements(MethodNode methodNode);
		
		/**
		 * Add the object from which this value came on top of the stack
		 * 
		 * @param toInsert list of instructions where the needed statements are added to at the end
		 */
		abstract void loadSourceObject(InsnList toInsert);

		/**
		 * Add the index from which this value came on top of the stack
		 * 
		 * @param toInsert list of instructions where the needed statements are added to at the end
		 */
		abstract void loadSourceIndex(InsnList toInsert);
	}
	
	/**
	 * A char received from a {@link String#charAt(int)} call. 
	 */
	private static class CharAtValue extends TrackableValue {
		/** The charAt() call */
		private final MethodInsnNode mInsn;
		
		/** Index of the local variable storing the target String */
		private int targetStringLocal;
		/** Index of the local variable storing the index */
		private int indexLocal;
		
		private CharAtValue(MethodInsnNode mInsn) {
			super(Type.CHAR_TYPE);
			this.mInsn = mInsn;
			// Note: when it can return something else than char, we get type with Type.getReturnType(mInsn.desc)
		}
		
		@Override
		void insertTrackStatements(MethodNode methodNode) {
			// on the stack before the charAt call: String target, int index
			
			targetStringLocal = methodNode.maxLocals++;
			indexLocal = methodNode.maxLocals++;
			
			InsnList toInsert = new InsnList();
			
			// we could avoid some local-to-stack copying by dupping
//			toInsert.add(new InsnNode(Opcodes.DUP2));
			
			toInsert.add(new VarInsnNode(Opcodes.ISTORE, indexLocal));
			toInsert.add(new VarInsnNode(Opcodes.ASTORE, targetStringLocal));
			
			toInsert.add(new VarInsnNode(Opcodes.ALOAD, targetStringLocal));
			toInsert.add(new VarInsnNode(Opcodes.ILOAD, indexLocal));
			
			methodNode.instructions.insertBefore(mInsn, toInsert);
		}
		
		@Override
		void loadSourceObject(InsnList toInsert) {
			toInsert.add(new VarInsnNode(Opcodes.ALOAD, targetStringLocal));
		}
		
		@Override
		void loadSourceIndex(InsnList toInsert) {
			toInsert.add(new VarInsnNode(Opcodes.ILOAD, indexLocal));
		}
	}
	
	private static final Type CHAR_ARRAY_TYPE = Type.getType("[C"); 
	
	public ClassVisitor createClassAdapter(ClassVisitor cv) {
		return new CharArrayClassAdapter(cv);
	}
}
