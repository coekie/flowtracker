package be.coekaerts.wouter.flowtracker.weaver.flow;

import be.coekaerts.wouter.flowtracker.weaver.ClassAdapterFactory;
import be.coekaerts.wouter.flowtracker.weaver.Types;
import java.util.ArrayList;
import java.util.List;
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

public class FlowAnalyzingTransformer implements ClassAdapterFactory {
	private static class FlowClassAdapter extends ClassVisitor {
		private String name;

		public FlowClassAdapter(ClassVisitor cv) {
			super(Opcodes.ASM5, cv);
		}
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			this.name = name;
		}
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			return new FlowMethodAdapter(mv, this.name, access, name, desc, signature, exceptions);
		}
	}
	
	private static class FlowMethodAdapter extends MethodNode {
		private final String owner;
		private final MethodVisitor mv;
		
		public FlowMethodAdapter(MethodVisitor mv, String owner, int access, String name, String desc, String signature, String[] exceptions) {
			super(Opcodes.ASM5, access, name, desc, signature, exceptions);
			this.owner = owner;
			this.mv = mv;
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			FlowInterpreter interpreter = new FlowInterpreter();
			Analyzer<BasicValue> analyzer = new Analyzer<>(interpreter);
			try {
				analyzer.analyze(owner, this);
			} catch (AnalyzerException e) {
				throw new RuntimeException(e);
			}
			
			Frame<BasicValue>[] frames = analyzer.getFrames();
			
			List<Store> stores = new ArrayList<>();
			
			for (int i = 0; i < instructions.size(); i++) {
				AbstractInsnNode insn = instructions.get(i);
				Frame<BasicValue> frame = frames[i];
				if (insn.getOpcode() == Opcodes.CASTORE) {
					stores.add(new CaStore((InsnNode)insn, frame));
				} else if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
					MethodInsnNode mInsn = (MethodInsnNode) insn;
					if ("java/lang/System".equals(mInsn.owner) && "arraycopy".equals(mInsn.name)
							&& "(Ljava/lang/Object;ILjava/lang/Object;II)V".equals(mInsn.desc)) {
						// if it is a copy from char[] to char[]
						if (Types.CHAR_ARRAY.equals(frame.getStack(frame.getStackSize() - 5).getType())
								&& Types.CHAR_ARRAY.equals((frame.getStack(frame.getStackSize() - 3)).getType())) {
							// replace it with a call to our hook instead
							mInsn.owner = "be/coekaerts/wouter/flowtracker/hook/SystemHook";
						}
					}
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
		public BasicValue newValue(Type type) {
			// for char[], remember the exact type
			if (CHAR_ARRAY_TYPE.equals(type)) {
				return new BasicValue(type);
			}
			// for others the exact type doesn't matter
			return super.newValue(type);
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public BasicValue naryOperation(AbstractInsnNode insn, List values) throws AnalyzerException {
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
