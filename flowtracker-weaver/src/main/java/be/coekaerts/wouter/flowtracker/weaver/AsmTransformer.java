package be.coekaerts.wouter.flowtracker.weaver;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class AsmTransformer implements ClassFileTransformer {
	
	public AsmTransformer() {
	}
	
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {

		// don't transform our own classes
		if (className.startsWith("be/coekaerts/wouter/flowtracker")) {
			return null;
		}
		
		try {
			if (className.equals("java/lang/String")) {
				ClassReader reader = new ClassReader(classfileBuffer);
				ClassWriter writer = new ClassWriter(0);
				StringAdapter adapter = new StringAdapter(writer);
				reader.accept(adapter, 0);
				byte[] result = writer.toByteArray();
		
				if (result.length != classfileBuffer.length) { // fast but dirty: assume every change changes the length
					System.out.println("AsmTransformer: Transformed " + className);
					return result;
				} else {
					System.out.println("AsmTransformer: No changes " + className);
					return null;
				}
			} else {
				return null;
			}
		} catch (Throwable t) {
			// TODO better logging
			t.printStackTrace(); // make sure the exception isn't silently ignored
			throw new RuntimeException("Exception while transforming class", t);
		}
	}

}
