package be.coekaerts.wouter.flowtracker.weaver;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

public class HookSpec {
	public static abstract class HookArgument {
		abstract void load(GeneratorAdapter generator);
		abstract Type getType(HookSpec hookSpec);
	}
	
	public static HookArgument THIS = new HookArgument() {
		@Override
		void load(GeneratorAdapter generator) {
			generator.loadThis();
		}
		
		@Override
		Type getType(HookSpec hookSpec) {
			return hookSpec.targetClass;
		}
	};
	
	private static class ArgHookArgument extends HookArgument {
		private final int index;

		public ArgHookArgument(int index) {
			this.index = index;
		}
		
		@Override
		public void load(GeneratorAdapter generator) {
			generator.loadArg(index);
		}
		
		@Override
		Type getType(HookSpec hookSpec) {
			return hookSpec.cacheTargetMethodArgumentTypes[index];
		}		
	}
	
	public static HookArgument ARG0 = new ArgHookArgument(0);
	public static HookArgument ARG1 = new ArgHookArgument(1);

	private class HookMethodAdapter extends AdviceAdapter {
		private HookMethodAdapter(MethodVisitor mv, int access, String name, String desc) {
			super(mv, access, name, desc);
		}
		
		@Override
		protected void onMethodExit(int opcode) {
			if (opcode != ATHROW) {
				// TODO only copy result if our hook doesn't override it
				dup(); // copy result
				for (HookArgument argument : hookArguments) {
					argument.load(this);
				}
				invokeStatic(Type.getType(hookClass), getHookMethod());
			}
		}
		
		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			// pessimistic upper limit: we push hookArguments on the stack
			super.visitMaxs(maxStack + hookArguments.length, maxLocals);
		}
	}
	
	private final Type targetClass;
//	private final Method targetMethod;
	private final Class<?> hookClass;
	private final HookArgument[] hookArguments;
	private final Method hookMethod;
	
	private final Type[] cacheTargetMethodArgumentTypes;
	
	public HookSpec(Type targetClass, Method targetMethod,
			Class<?> hookClass, Method hookMethod, HookArgument... hookArguments) {
		super();
		this.targetClass = targetClass;
//		this.targetMethod = targetMethod;
		this.hookClass = hookClass;
		this.hookMethod = hookMethod;
		this.hookArguments = hookArguments;
		
		this.cacheTargetMethodArgumentTypes = targetMethod.getArgumentTypes();
	}

//	private Class<?>[] getHookParameterClasses() throws ClassNotFoundException {
//		Class<?>[] types = new Class<?>[hookArguments.length + 1];
//		
//		types[0] = Class.forName(targetMethod.getReturnType().getClassName());
//		for (int i = 0; i < hookArguments.length ; i++) {
//			types[i+1] = Class.forName(hookArguments[i].getType(this).getClassName()); 
//		}
//		
//		return types;
//	}
	
	private Method getHookMethod() {
		return hookMethod;
//		try {
//			return Method.getMethod(hookClass.getDeclaredMethod(hookName, getHookParameterClasses()));
//		} catch (Exception e) {
//			throw new RuntimeException(e);
//		}
	}
	
	public MethodVisitor createMethodAdapter(MethodVisitor mv, int access, String name, String desc) {
		return new HookMethodAdapter(mv, access, name, desc);
	}
}
