package be.coekaerts.wouter.flowtracker.weaver;

import be.coekaerts.wouter.flowtracker.hook.InputStreamReaderHook;
import be.coekaerts.wouter.flowtracker.hook.URLConnectionHook;
import be.coekaerts.wouter.flowtracker.weaver.flow.FlowAnalyzingTransformer;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

@SuppressWarnings("UnusedDeclaration") // loaded by name by the agent
public class AsmTransformer implements ClassFileTransformer {
	private final Map<String, ClassHookSpec> specs = new HashMap<String, ClassHookSpec>();
	
	public AsmTransformer() {
		ClassHookSpec inputStreamReaderSpec = new ClassHookSpec(Type.getType("Ljava/io/InputStreamReader;"), InputStreamReaderHook.class);

    inputStreamReaderSpec.addMethodHookSpec("void <init>(java.io.InputStream)", "void afterInit(java.io.InputStreamReader,java.io.InputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    inputStreamReaderSpec.addMethodHookSpec("void <init>(java.io.InputStream, java.lang.String)", "void afterInit(java.io.InputStreamReader,java.io.InputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    inputStreamReaderSpec.addMethodHookSpec("void <init>(java.io.InputStream, java.nio.charset.Charset)", "void afterInit(java.io.InputStreamReader,java.io.InputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    inputStreamReaderSpec.addMethodHookSpec("void <init>(java.io.InputStream, java.nio.charset.CharsetDecoder)", "void afterInit(java.io.InputStreamReader,java.io.InputStream)",
        HookSpec.THIS, HookSpec.ARG0);

		inputStreamReaderSpec.addMethodHookSpec("int read()", "void afterRead1(int,java.io.InputStreamReader)", HookSpec.THIS);
		inputStreamReaderSpec.addMethodHookSpec("int read(char[],int,int)", "void afterReadCharArrayOffset(int,java.io.InputStreamReader,char[],int)",
				HookSpec.THIS, HookSpec.ARG0, HookSpec.ARG1);
    // we assume the other methods ultimately delegate to the ones we hooked
		specs.put("java/io/InputStreamReader", inputStreamReaderSpec);
	}

  private ClassHookSpec getSpec(String className) {
    if (className.endsWith("URLConnection")) {
      return urlConnectionHook(className);
    }
    return specs.get(className);
  }

  private ClassHookSpec urlConnectionHook(String urlConnectionSubclass) {
    ClassHookSpec spec = new ClassHookSpec(
        Type.getType('L' + urlConnectionSubclass.replace('.', '/') + ';'), URLConnectionHook.class);
    spec.addMethodHookSpec("java.io.InputStream getInputStream()",
        "void afterGetInputStream(java.io.InputStream,java.net.URLConnection)", HookSpec.THIS);
    return spec;
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
      ClassHookSpec spec = getSpec(className);
      if (spec != null) {
				adapterFactory = spec;
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
			reader.accept(adapter, ClassReader.EXPAND_FRAMES);
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
