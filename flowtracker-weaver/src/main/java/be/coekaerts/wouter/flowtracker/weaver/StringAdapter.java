package be.coekaerts.wouter.flowtracker.weaver;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import be.coekaerts.wouter.flowtracker.hook.StringHook;

public class StringAdapter extends ClassAdapter {
	private final Class<?> hookClass = StringHook.class;
	
	private final Method concat = Method.getMethod("String concat(String)");
	private final Method substring = Method.getMethod("String substring(int,int)");

	public StringAdapter(ClassVisitor cv) {
		super(cv);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		if (isMethod(concat, name, desc)) {
			return new ConcatAdapter(mv, access, name, desc);
		} else if (isMethod(substring, name, desc)) {
			return new SubstringAdapter(mv, access, name, desc);
		} else {
			return mv;
		}
	}
	
	
	private class ConcatAdapter extends AdviceAdapter {
		public ConcatAdapter(MethodVisitor mv, int access, String name, String desc) {
			super(mv, access, name, desc);
		}
		
		@Override
		protected void onMethodExit(int opcode) {
			if (opcode == ARETURN) {
				// StringHook.afterConcat(result, this, arg0)
				loadThis();
				loadArg(0);
				invokeHook(this, "afterConcat", String.class, String.class, String.class);
			}
		}
		
		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			super.visitMaxs(maxStack + 3, maxLocals);
		}
	}
	
	private class SubstringAdapter extends AdviceAdapter {
		public SubstringAdapter(MethodVisitor mv, int access, String name, String desc) {
			super(mv, access, name, desc);
		}
		
		@Override
		protected void onMethodExit(int opcode) {
			if (opcode == ARETURN) {
				// StringHook.afterSubstring(result, this, arg0, arg1)
				dup(); // copy result
				loadThis();
				loadArg(0);
				loadArg(1);
				invokeHook(this, "afterSubstring", String.class, String.class, int.class, int.class);
			}
		}
		
		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			super.visitMaxs(maxStack + 4, maxLocals);
		}
	}
	
	private boolean isMethod(Method method, String name, String desc) {
		return method.getName().equals(name) && method.getDescriptor().equals(desc);
	}
	
	private void invokeHook(GeneratorAdapter adapter, String methodName, Class<?>... parameterTypes) {
		try {
			Type hookType = Type.getType(hookClass);
			Method method = Method.getMethod(hookClass.getDeclaredMethod(methodName, parameterTypes));
			adapter.invokeStatic(hookType, method);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
