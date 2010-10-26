package be.coekaerts.wouter.flowtracker.weaver;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
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
import org.objectweb.asm.tree.analysis.Frame;
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
			
			Frame[] frames = analyzer.getFrames();
			
			List<Store> stores = new ArrayList<Store>();
			
			for (int i = 0; i < instructions.size(); i++) {
				AbstractInsnNode insn = instructions.get(i);
				Frame frame = frames[i];
				if (insn.getOpcode() == Opcodes.CASTORE) {
					stores.add(new CaStore((InsnNode)insn, frame));
					// TODO shouldn't need to create an instance for that
				}
			}
			
			for (Store store : stores) {
				store.insertTrackStatements(this);
			}
			
			this.accept(mv); // send the result to the next MethodVisitor
		}
	}
	
	private static class CharArrayInterpreter extends BasicInterpreter {

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
	}
	
	/**
	 * A value of which we can track where it came from
	 */
	static abstract class TrackableValue extends BasicValue {
		private boolean tracked;
		
		private TrackableValue(Type type) {
			super(type);
		}
		
		void ensureTracked(MethodNode methodNode) {
			if (! tracked) {
				insertTrackStatements(methodNode);
				tracked = true;
			}
		}
		
		/**
		 * Insert the statements needed to keep track of the origin of this value.
		 * 
		 * This method should not be called directly, instead {@link #ensureTracked(MethodNode)} should be
		 * used, to ensure statements are inserted more than once.
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
