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

import be.coekaerts.wouter.flowtracker.hook.InputStreamReaderHook;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer;

public class AsmTransformer implements ClassFileTransformer {
	private final Map<String, ClassHookSpec> specs = new HashMap<String, ClassHookSpec>();
	
	public AsmTransformer() {
		ClassHookSpec inputStreamReaderSpec = new ClassHookSpec(Type.getType("Ljava/io/InputStreamReader;"), InputStreamReaderHook.class);
		inputStreamReaderSpec.addMethodHookSpec("int read()", "void afterRead1(int,java.io.InputStreamReader)", HookSpec.THIS);
//		inputStreamReaderSpec.addMethodHookSpec("int read(char[])", "void afterReadCharArray(int,java.io.InputStreamReader,char[])",
//				HookSpec.THIS, HookSpec.ARG0, HookSpec.ARG1, HookSpec.ARG2);
		inputStreamReaderSpec.addMethodHookSpec("int read(char[],int,int)", "void afterReadCharArrayOffset(int,java.io.InputStreamReader,char[],int)",
				HookSpec.THIS, HookSpec.ARG0, HookSpec.ARG1);
		specs.put("java/io/InputStreamReader", inputStreamReaderSpec);
	}
	
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {

		// don't transform our own classes
		if (className.startsWith("be/coekaerts/wouter/flowtracker")
				&& ! className.startsWith("be/coekaerts/wouter/flowtracker/test")) {
			return null;
		}
		
		try {
			ClassAdapterFactory adapterFactory;
			if (specs.containsKey(className)) {
				adapterFactory = specs.get(className);
			} else if (className.startsWith("be/coekaerts/wouter/flowtracker/test/")
					|| className.equals("java/util/Arrays")
					|| className.equals("java/lang/String")
					|| className.equals("java/lang/AbstractStringBuilder")) {
				adapterFactory = new FlowAnalyzingTransformer();
			} else {
				return null;
			}
				
			ClassReader reader = new ClassReader(classfileBuffer);
			ClassWriter writer = new ClassWriter(0);
			ClassVisitor adapter = adapterFactory.createClassAdapter(writer);
			if (className.equals("java/lang/String")) {
				adapter = new StringAdapter(adapter);
			}
			reader.accept(adapter, 0);
			byte[] result = writer.toByteArray();
	
			System.out.println("AsmTransformer: Transformed " + className);
			return result;
		} catch (Throwable t) {
			// TODO better logging
			t.printStackTrace(); // make sure the exception isn't silently ignored
			throw new RuntimeException("Exception while transforming class", t);
		}
	}

}
