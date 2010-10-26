package be.coekaerts.wouter.flowtracker.weaver.flow;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;

import be.coekaerts.wouter.flowtracker.weaver.ClassAdapterFactory;

public class FlowAnalyzingTransformer implements ClassAdapterFactory {
	private static class FlowClassAdapter extends ClassAdapter {
		private String name;

		public FlowClassAdapter(ClassVisitor cv) {
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
			FlowMethodAdapter methodAdapter = new FlowMethodAdapter(mv, this.name, access, name, desc, signature, exceptions);
			return methodAdapter;
		}
	}
	
	private static class FlowMethodAdapter extends MethodNode {
		private final String owner;
		private final MethodVisitor mv;
		
		public FlowMethodAdapter(MethodVisitor mv, String owner, int access, String name, String desc, String signature, String[] exceptions) {
			super(access, name, desc, signature, exceptions);
			this.owner = owner;
			this.mv = mv;
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			FlowInterpreter interpreter = new FlowInterpreter();
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
	
	private static class FlowInterpreter extends BasicInterpreter {

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
	
	private static final Type CHAR_ARRAY_TYPE = Type.getType("[C"); 
	
	public ClassVisitor createClassAdapter(ClassVisitor cv) {
		return new FlowClassAdapter(cv);
	}
}
