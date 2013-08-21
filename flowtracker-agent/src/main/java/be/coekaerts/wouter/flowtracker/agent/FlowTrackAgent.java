package be.coekaerts.wouter.flowtracker.agent;

import java.io.InputStreamReader;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

public class FlowTrackAgent {
	private static final Map<String, String> config = new HashMap<String, String>();

	public static void premain(String agentArgs, Instrumentation inst) {
		System.out.println("FlowTrackAgent.premain");

		try {
			initProperties(agentArgs);

			JarFile aspectJar = new JarFile(getPath("core"));

			// make the instrumented JDK classes find the hook class
			// troubleshooting hint: there's also appendToSystemClassLoaderSearch
			inst.appendToBootstrapClassLoaderSearch(aspectJar);

			inst.addTransformer(createTransformer(), true);

			// TODO avoid hardcoding of list of classes to retransform here
			inst.retransformClasses(String.class);
			inst.retransformClasses(InputStreamReader.class);
			inst.retransformClasses(Arrays.class);
			inst.retransformClasses(StringBuilder.class.getSuperclass()); // AbstractStringBuilder is not public
		} catch (Throwable e) {
			e.printStackTrace();
			System.exit(1);
			throw new RuntimeException(e); // make compiler happy
		}
	 }

	/**
	 * Create the transformer. It is loaded with its dependencies in a separate class loader to avoid
	 * polluting to bootstrap classpath
	 */
	private static ClassFileTransformer createTransformer() throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		ClassLoader classLoader = new URLClassLoader(new URL[]{
				new URL("file://" + getPath("weaver")),
				new URL("file://" + getPath("asm")),
		});
		Class<?> transformerClass = classLoader.loadClass("be.coekaerts.wouter.flowtracker.weaver.AsmTransformer");
		return (ClassFileTransformer) transformerClass.newInstance();
	}

	private static void initProperties(String agentArgs) {
		if (agentArgs == null) {
			throw new RuntimeException("You must provide arguments to the agent");
		}
//		System.err.println("FlowTrackAgent: agentArgs: " + agentArgs);
		for (String arg : agentArgs.split(";")) {
			String[] keyAndValue = arg.split("=", 2);
			config.put(keyAndValue[0], keyAndValue[1]);
		}
	}

	private static String getPath(String name) {
		// TODO default path
		String result = config.get(name);
		if (result == null) {
			throw new RuntimeException("You must provide '" + name + "' as argument to the agent");
		}
		return result;
	}
}
