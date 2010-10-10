package be.coekaerts.wouter.flowtracker.weaver;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import be.coekaerts.wouter.flowtracker.weaver.HookSpec.HookArgument;

public class ClassHookSpec {
	private class HookClassAdapter extends ClassAdapter {
		private HookClassAdapter(ClassVisitor cv) {
			super(cv);
		}
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			
			Method method = new Method(name, desc);
			HookSpec hookSpec = methodHookSpecs.get(method);
			if (hookSpec != null) {
				return hookSpec.createMethodAdapter(mv, access, name, desc);
			} else {
				return mv;
			}
		}
	}
	
	private final Type targetClass;
	private final Class<?> hookClass;
	
	private Map<Method, HookSpec> methodHookSpecs = new HashMap<Method, HookSpec>();
	
	public ClassHookSpec(Type targetClass, Class<?> hookClass) {
		this.targetClass = targetClass;
		this.hookClass = hookClass;
	}
	
	public void addMethodHookSpec(String targetMethod, String hookMethod, HookArgument... hookArguments) {
		addMethodHookSpec(Method.getMethod(targetMethod), Method.getMethod(hookMethod), hookArguments);
	}
	
	public void addMethodHookSpec(Method targetMethod, Method hookMethod, HookArgument... hookArguments) {
		HookSpec hookSpec = new HookSpec(targetClass, targetMethod, hookClass, hookMethod, hookArguments);
		methodHookSpecs.put(targetMethod, hookSpec);
	}
	
	public ClassVisitor createClassAdapter(ClassVisitor cv) {
		return new HookClassAdapter(cv);
	}
}
