package be.coekaerts.wouter.flowtracker.weaver;

import be.coekaerts.wouter.flowtracker.weaver.HookSpec.HookArgument;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class ClassHookSpec implements ClassAdapterFactory {
	private class HookClassAdapter extends ClassVisitor {
		private HookClassAdapter(ClassVisitor cv) {
			super(Opcodes.ASM4, cv);
		}
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			
			Method method = new Method(name, desc);
			HookSpec hookSpec = methodHookSpecs.get(method);
			if (hookSpec != null) {
				System.out.println("ClassHookSpec: Transforming " + targetClass.getClassName() + "." + name + desc);
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
