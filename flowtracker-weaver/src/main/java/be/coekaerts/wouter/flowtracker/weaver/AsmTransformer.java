package be.coekaerts.wouter.flowtracker.weaver;

import be.coekaerts.wouter.flowtracker.hook.InputStreamReaderHook;
import be.coekaerts.wouter.flowtracker.hook.OutputStreamWriterHook;
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
import org.objectweb.asm.util.CheckClassAdapter;

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

    ClassHookSpec outputStreamWriterSpec = new ClassHookSpec(
        Type.getType("Ljava/io/OutputStreamWriter;"), OutputStreamWriterHook.class);
    outputStreamWriterSpec.addMethodHookSpec("void <init>(java.io.OutputStream)",
        "void afterInit(java.io.OutputStreamWriter,java.io.OutputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    outputStreamWriterSpec.addMethodHookSpec("void <init>(java.io.OutputStream,java.lang.String)",
        "void afterInit(java.io.OutputStreamWriter,java.io.OutputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    outputStreamWriterSpec.addMethodHookSpec(
        "void <init>(java.io.OutputStream,java.nio.charset.Charset)",
        "void afterInit(java.io.OutputStreamWriter,java.io.OutputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    outputStreamWriterSpec.addMethodHookSpec(
        "void <init>(java.io.OutputStream,java.nio.charset.CharsetEncoder)",
        "void afterInit(java.io.OutputStreamWriter,java.io.OutputStream)",
        HookSpec.THIS, HookSpec.ARG0);
    outputStreamWriterSpec.addMethodHookSpec("void write(int)",
        "void afterWrite1(java.io.OutputStreamWriter, int)", HookSpec.THIS, HookSpec.ARG0);
    outputStreamWriterSpec.addMethodHookSpec("void write(char[],int,int)",
        "void afterWriteCharArrayOffset(java.io.OutputStreamWriter,char[],int,int)",
        HookSpec.THIS, HookSpec.ARG0, HookSpec.ARG1, HookSpec.ARG2);
    outputStreamWriterSpec.addMethodHookSpec("void write(java.lang.String,int,int)",
        "void afterWriteStringOffset(java.io.OutputStreamWriter,java.lang.String,int,int)",
        HookSpec.THIS, HookSpec.ARG0, HookSpec.ARG1, HookSpec.ARG2);
    specs.put("java/io/OutputStreamWriter", outputStreamWriterSpec);
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
      // NICE make checkAdapter optional; for development only
      CheckClassAdapter checkAdapter = new CheckClassAdapter(writer);
      ClassVisitor adapter = adapterFactory.createClassAdapter(checkAdapter);
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
