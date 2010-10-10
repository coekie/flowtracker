package be.coekaerts.wouter.flowtracker.weaver;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import be.coekaerts.wouter.flowtracker.hook.StringHook;

public class AsmTransformer implements ClassFileTransformer {
	private final Map<String, ClassHookSpec> specs = new HashMap<String, ClassHookSpec>();
//	private final ClassHookSpec stringSpec;	
	
	public AsmTransformer() {
		ClassHookSpec stringSpec = new ClassHookSpec(Type.getType("Ljava/lang/String;"), StringHook.class);
		stringSpec.addMethodHookSpec("String concat(String)", "String afterConcat(String,String,String)",
				HookSpec.THIS, HookSpec.ARG0);
		stringSpec.addMethodHookSpec("String substring(int,int)", "String afterSubstring(String,String,int,int)",
				HookSpec.THIS, HookSpec.ARG0, HookSpec.ARG1);
		specs.put("java/lang/String", stringSpec);
		
		
	}
	
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {

		// don't transform our own classes
		if (className.startsWith("be/coekaerts/wouter/flowtracker")) {
			return null;
		}
		
		try {
			ClassHookSpec spec = specs.get(className);
			if (spec == null) {
				return null;
			} else {
				ClassReader reader = new ClassReader(classfileBuffer);
				ClassWriter writer = new ClassWriter(0);
				ClassVisitor adapter = spec.createClassAdapter(writer);
				reader.accept(adapter, 0);
				byte[] result = writer.toByteArray();
		
				System.out.println("AsmTransformer: Transformed " + className);
				return result;
			}
		} catch (Throwable t) {
			// TODO better logging
			t.printStackTrace(); // make sure the exception isn't silently ignored
			throw new RuntimeException("Exception while transforming class", t);
		}
	}

}
